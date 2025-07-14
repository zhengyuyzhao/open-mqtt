package com.zzy.mqtt.broker.cluster;

import cn.hutool.core.collection.CollectionUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.zzy.mqtt.logic.service.internal.IInternalMessageService;
import com.zzy.mqtt.logic.service.internal.InternalMessageDTO;
import com.zzy.mqtt.logic.service.store.IMessageIdService;
import com.zzy.mqtt.logic.service.store.IServerPublishMessageStoreService;
import com.zzy.mqtt.logic.service.store.ISubscribeStoreService;
import com.zzy.mqtt.logic.service.store.SubscribeStoreDTO;
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
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
public class VertxClusterInternalMessageService implements IInternalMessageService {

    private static final String INTERNAL_MESSAGE_TOPIC_PREFIX = "internal.message.";
    private EventBus eb;
    private ConcurrentHashMap<String, MessageConsumer> consumerMap = new ConcurrentHashMap<>();

    private ObjectMapper objectMapper = new ObjectMapper();

    private ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor();
    private final ITransportLocalStoreService transportLocalStoreService;
    private final IClientStoreService transportStoreService;
    private final ISubscribeStoreService subscribeStoreService;

    private final IServerPublishMessageStoreService serverPublishMessageStoreService;

    private final IMessageIdService messageIdService;

    private final String brokerId;

    private final Cache<String, String> transportBrokerCache;

    private final Cache<String, List<SubscribeStoreDTO>> subscribeCache;

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

        subscribeCache = Caffeine.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(Duration.ofMillis(5000))
                .build();

        Vertx vertx = vertxCluster.getVertx();
        eb = vertx.eventBus();
        init();
    }

    @Override
    public void internalPublish(InternalMessageDTO internalMessageDTO) {
        log.info("Publishing internal message: {}", internalMessageDTO);
        executorService.execute(() -> {
            publishInner(internalMessageDTO);
        });

    }

    @SneakyThrows
    private void publishInner(InternalMessageDTO internalMessageDTO) {

        List<SubscribeStoreDTO> subscribeStoreDTOS = getSubscribeStoreDTOS(internalMessageDTO.getTopic());
        if (subscribeStoreDTOS == null) return;

        for (SubscribeStoreDTO subscribeStoreDTO : subscribeStoreDTOS) {
            internalMessageDTO.setMessageId(messageIdService.getNextMessageId(subscribeStoreDTO.getClientId()));
            internalMessageDTO.setToClientId(subscribeStoreDTO.getClientId());
            internalMessageDTO.setMqttQoS(subscribeStoreDTO.getMqttQoS());
            if (transportLocalStoreService.getTransport(subscribeStoreDTO.getClientId()) != null) {
                // If the transport exists, publish to the specific broker
                log.info("Publishing internal message to local broker client: {}", internalMessageDTO.getToClientId());
                internalSubscribe(internalMessageDTO);
                continue;
            }

            String broker = getTransportBroker(subscribeStoreDTO.getClientId());
            if (!StringUtil.isNullOrEmpty(broker)) {
                // If the transport exists in the store, publish to the specific broker
                log.info("Publishing internal message to specific broker to broker: {}", broker);
                eb.send(INTERNAL_MESSAGE_TOPIC_PREFIX + broker, objectMapper.writeValueAsString(internalMessageDTO));
            } else {
                eb.publish(INTERNAL_MESSAGE_TOPIC_PREFIX, objectMapper.writeValueAsString(internalMessageDTO));
            }

        }

//        String json = objectMapper.writeValueAsString(internalMessageDTO);
//        String broker = transportStoreService.getBroker(internalMessageDTO.getClientId());
//        if (!StringUtil.isNullOrEmpty(broker)) {
//            // If the transport exists in the store, publish to the specific broker
//            log.info("Publishing internal message to specific broker from store: {}", broker);
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
                        log.info("Transport Topic:{} to clientId: {}， message;{}", internalMessageDTO.getTopic(),
                                internalMessageDTO.getToClientId(), internalMessageDTO);
                        transport.publish(internalMessageDTO.getTopic(),
                                internalMessageDTO.getMessageBytes(),
                                MqttQoS.valueOf(internalMessageDTO.getMqttQoS()),
                                internalMessageDTO.isRetain(),
                                internalMessageDTO.isDup(),
                                internalMessageDTO.getMessageId()
                        );
                        if (internalMessageDTO.getMqttQoS() == 0) {
                            serverPublishMessageStoreService.remove(internalMessageDTO.getToClientId(),
                                    internalMessageDTO.getMessageId());
                        }

                    }
                } else {
                    // TODO 保留逻辑
                    log.error("Internal message without clientId: {}", internalMessageDTO);
                }
            } catch (Exception e) {
                log.error("Error processing internal message: {}", internalMessageDTO, e);
            }
        });

    }

    private String getTransportBroker(String clientId) {
        if (transportBrokerCache.getIfPresent(clientId) != null) {
            return transportBrokerCache.getIfPresent(clientId);
        }
        String broker = transportStoreService.getBroker(clientId);
        transportBrokerCache.put(clientId, broker);
        return broker;
    }

    @Nullable
    private List<SubscribeStoreDTO> getSubscribeStoreDTOS(String topic) {
        if (subscribeCache.getIfPresent(topic) != null) {
            return subscribeCache.getIfPresent(topic);
        }
        List<SubscribeStoreDTO> subscribeStoreDTOS = subscribeStoreService.search(topic);
        if (CollectionUtil.isEmpty(subscribeStoreDTOS)) {
            log.info("No subscribers found for topic: {}", topic);
            return null;
        }
        subscribeCache.put(topic, subscribeStoreDTOS);
        return subscribeStoreDTOS;
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
        log.info("Registered internal message listener");
        consumerMap.put(INTERNAL_MESSAGE_TOPIC_PREFIX, consumer);
        consumerMap.put(INTERNAL_MESSAGE_TOPIC_PREFIX + brokerId, point2PointConsumer);
    }

//    @Override
//    public void unregisterInternalMessageListener(String topic) {
//        MessageConsumer consumer = consumerMap.remove(topic);
//        if (consumer != null) {
//            consumer.unregister();
//            log.info("Unregistered internal message listener for topic: {}", topic);
//        } else {
//            log.warn("No internal message listener found for topic: {}", topic);
//        }
//    }
}
