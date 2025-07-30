package com.zzy.mqtt.broker.cluster;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.zzy.mqtt.logic.service.internal.IInternalMessageService;
import com.zzy.mqtt.logic.service.internal.InternalMessageDTO;
import com.zzy.mqtt.logic.service.store.IMessageIdService;
import com.zzy.mqtt.logic.service.store.IServerPublishMessageStoreService;
import com.zzy.mqtt.logic.service.store.ISubscribeStoreService;
import com.zzy.mqtt.logic.service.transport.IClientStoreService;
import com.zzy.mqtt.logic.service.transport.ITransport;
import com.zzy.mqtt.logic.service.transport.ITransportLocalStoreService;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.util.internal.StringUtil;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.MessageConsumer;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.concurrent.*;

@Slf4j
public class VertxClusterInternalMessageService implements IInternalMessageService {

    private static final String INTERNAL_MESSAGE_TOPIC_PREFIX = "internal.message.";
    private EventBus eb;
    private ConcurrentHashMap<String, MessageConsumer> consumerMap = new ConcurrentHashMap<>();

    private ObjectMapper objectMapper = new ObjectMapper();

    private ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor();

    private ExecutorService storeService = new ThreadPoolExecutor(
            10, 10, 100L, TimeUnit.SECONDS,
            new java.util.concurrent.LinkedBlockingQueue<>(10000),
            runnable -> new Thread(runnable, "VertxClusterInternalMessageService-StoreService")
    );
    private final ITransportLocalStoreService transportLocalStoreService;
    private final IClientStoreService transportStoreService;
    private final ISubscribeStoreService subscribeStoreService;

    private final IServerPublishMessageStoreService serverPublishMessageStoreService;

    private final IMessageIdService messageIdService;

    private final String brokerId;

    private final Cache<String, String> transportBrokerCache;


    public VertxClusterInternalMessageService(String brokerId, ITransportLocalStoreService transportLocalStoreService,
                                              IClientStoreService transportStoreService,
                                              ISubscribeStoreService subscribeStoreService,
                                              IServerPublishMessageStoreService serverPublishMessageStoreService,
                                              IMessageIdService messageIdService,
                                              VertxCluster vertxCluster) {
        this.brokerId = brokerId;
        this.transportLocalStoreService = transportLocalStoreService;
        this.transportStoreService = transportStoreService;
        this.subscribeStoreService = subscribeStoreService;
        this.serverPublishMessageStoreService = serverPublishMessageStoreService;
        this.messageIdService = messageIdService;

        transportBrokerCache = Caffeine.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(Duration.ofMillis(5000))
                .build();


        Vertx vertx = vertxCluster.getVertx();
        eb = vertx.eventBus();
        init();
    }

    public ExecutorService getStoreService() {
        return storeService;
    }

    @Override
    public void internalPublish(InternalMessageDTO internalMessageDTO) {
        log.debug("Publishing internal message: {}", internalMessageDTO);
        try {
            executorService.execute(() -> {
                publishInner(internalMessageDTO);
            });
        } catch (Exception e) {
            log.error("Error publishing internal message: {}", internalMessageDTO);
        }


    }

    @SneakyThrows
    private void publishInner(InternalMessageDTO internalMessageDTO) {
        if (transportLocalStoreService.getTransport(internalMessageDTO.getToClientId()) != null) {
            // If the transport exists, publish to the specific broker
            log.debug("Publishing internal message to local broker client: {}", internalMessageDTO.getToClientId());
            internalSubscribe(internalMessageDTO);
            return;
        }

        String broker = getTransportBroker(internalMessageDTO.getToClientId());

        if (StrUtil.isNotEmpty(broker)) {
            // If the transport exists in the store, publish to the specific broker
            log.debug("Publishing internal message to specific broker to broker: {}", broker);
            eb.send(INTERNAL_MESSAGE_TOPIC_PREFIX + broker, objectMapper.writeValueAsString(internalMessageDTO));
        }


//        else {
//            eb.publish(INTERNAL_MESSAGE_TOPIC_PREFIX, objectMapper.writeValueAsString(internalMessageDTO));
//        }


//        String json = objectMapper.writeValueAsString(internalMessageDTO);
//        String broker = transportStoreService.getBroker(internalMessageDTO.getClientId());
//        if (!StringUtil.isNullOrEmpty(broker)) {
//            // If the transport exists in the store, publish to the specific broker
//            log.debug("Publishing internal message to specific broker from store: {}", broker);
//            eb.send(INTERNAL_MESSAGE_TOPIC_PREFIX + broker, json);
//            return;
//        }
//
//        eb.publish(INTERNAL_MESSAGE_TOPIC_PREFIX, json);
    }

    @Override
    public void internalSubscribe(InternalMessageDTO internalMessageDTO) {
        executorService.execute(() -> {
            try {
                if (!StringUtil.isNullOrEmpty(internalMessageDTO.getToClientId())) {
                    ITransport transport = transportLocalStoreService.getTransport(internalMessageDTO.getToClientId());
                    if (transport != null) {
                        log.debug("Transport Topic:{} to clientId: {}， message;{}", internalMessageDTO.getTopic(),
                                internalMessageDTO.getToClientId(), internalMessageDTO);
                        transport.publish(internalMessageDTO.getTopic(),
                                internalMessageDTO.getMessageBytes(),
                                MqttQoS.valueOf(internalMessageDTO.getMqttQoS()),
                                internalMessageDTO.isRetain(),
                                internalMessageDTO.isDup(),
                                internalMessageDTO.getMessageId()
                        );
                    }
                } else {
                    // TODO 保留逻辑
                    log.error("Internal message without clientId: {}", internalMessageDTO);
                }
            } catch (Exception e) {
                log.error("Error processing internal message: {}", e.getMessage());
            }
        });

    }

    @SneakyThrows
    private String getTransportBroker(String clientId) {
        if (transportBrokerCache.getIfPresent(clientId) != null) {
            return transportBrokerCache.getIfPresent(clientId);
        }
        String broker = storeService.submit(() -> {
            try {
                return transportStoreService.getBroker(clientId);
            } catch (Exception e) {
                return null;
            }
        }).get();

        if (StrUtil.isNotEmpty(broker)) {
            transportBrokerCache.put(clientId, broker);
        }

        return broker;
    }


    public void init() {
        MessageConsumer<String> consumer = eb.consumer(INTERNAL_MESSAGE_TOPIC_PREFIX, message -> {
            try {
                InternalMessageDTO dto = objectMapper.readValue(message.body(), InternalMessageDTO.class);
                internalSubscribe(dto);
            } catch (Exception e) {
                log.error("Error processing internal message {}", e);
            }
        });
        MessageConsumer<String> point2PointConsumer = eb.consumer(INTERNAL_MESSAGE_TOPIC_PREFIX + brokerId, message -> {
            try {
                InternalMessageDTO dto = objectMapper.readValue(message.body(), InternalMessageDTO.class);
                internalSubscribe(dto);
            } catch (Exception e) {
                log.error("Error processing internal message {}", e);
            }
        });
        log.debug("Registered internal message listener");
        consumerMap.put(INTERNAL_MESSAGE_TOPIC_PREFIX, consumer);
        consumerMap.put(INTERNAL_MESSAGE_TOPIC_PREFIX + brokerId, point2PointConsumer);
    }

//    @Override
//    public void unregisterInternalMessageListener(String topic) {
//        MessageConsumer consumer = consumerMap.remove(topic);
//        if (consumer != null) {
//            consumer.unregister();
//            log.debug("Unregistered internal message listener for topic: {}", topic);
//        } else {
//            log.warn("No internal message listener found for topic: {}", topic);
//        }
//    }
}
