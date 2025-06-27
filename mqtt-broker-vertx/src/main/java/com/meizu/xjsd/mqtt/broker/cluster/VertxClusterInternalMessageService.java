package com.meizu.xjsd.mqtt.broker.cluster;

import cn.hutool.core.collection.CollectionUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meizu.xjsd.mqtt.logic.service.internal.IInternalMessageService;
import com.meizu.xjsd.mqtt.logic.service.internal.InternalMessageDTO;
import com.meizu.xjsd.mqtt.logic.service.store.ISubscribeStoreService;
import com.meizu.xjsd.mqtt.logic.service.store.SubscribeStoreDTO;
import com.meizu.xjsd.mqtt.logic.service.transport.ITransport;
import com.meizu.xjsd.mqtt.logic.service.transport.ITransportLocalStoreService;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
public class VertxClusterInternalMessageService implements IInternalMessageService {

    private static final String INTERNAL_MESSAGE_TOPIC_PREFIX = "internal.message";
    private EventBus eb;
    private ConcurrentHashMap<String, MessageConsumer> consumerMap = new ConcurrentHashMap<>();

    private ObjectMapper objectMapper = new ObjectMapper();

    private ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor();
    private final ITransportLocalStoreService transportLocalStoreService;
    private final ISubscribeStoreService subscribeStoreService;

    private final String brokerId;

    public VertxClusterInternalMessageService(String brokerId, ITransportLocalStoreService transportLocalStoreService,
                                              ISubscribeStoreService subscribeStoreService,
                                              VertxCluster vertxCluster) {
        this.brokerId = brokerId;
        this.transportLocalStoreService = transportLocalStoreService;
        this.subscribeStoreService = subscribeStoreService;
        Vertx vertx = vertxCluster.getVertx();
        eb = vertx.eventBus();
        init();
    }

    @Override
    public void internalPublish(InternalMessageDTO internalMessageDTO) throws Exception{
        log.info("Publishing internal message: {}", internalMessageDTO);
        String json = objectMapper.writeValueAsString(internalMessageDTO);
        eb.publish(INTERNAL_MESSAGE_TOPIC_PREFIX, json);

    }

    @Override
    public void internalSubscribe(InternalMessageDTO internalMessageDTO) {
        executorService.execute(() -> {
            try {
                List<SubscribeStoreDTO> subscribeStoreDTOS =  subscribeStoreService.search(internalMessageDTO.getTopic());
                if (CollectionUtil.isEmpty(subscribeStoreDTOS)) {
                    log.info("No subscribers found for topic: {}", internalMessageDTO.getTopic());
                    return;
                }
                for (SubscribeStoreDTO subscribeStoreDTO : subscribeStoreDTOS) {
                    ITransport transport = transportLocalStoreService.getTransport(subscribeStoreDTO.getClientId());
                    if (transport == null) {
                        log.info("Transport not found for clientId: {}", subscribeStoreDTO.getClientId());
                        return;
                    }
                    log.info("Publishing internal message to clientId: {}, topic: {}", subscribeStoreDTO.getClientId(), internalMessageDTO.getTopic());
                    transport.publish(internalMessageDTO.getTopic(),
                            internalMessageDTO.getMessageBytes(),
                            MqttQoS.valueOf(internalMessageDTO.getMqttQoS()),
                            internalMessageDTO.isRetain(),
                            internalMessageDTO.isDup());
                }
            } catch (Exception e) {
                log.error("Error processing internal message: {}", internalMessageDTO, e);
            }
        });

    }


    public void init() {
        MessageConsumer<String> consumer =  eb.consumer(INTERNAL_MESSAGE_TOPIC_PREFIX, message -> {
            try {
                InternalMessageDTO dto = objectMapper.readValue(message.body(), InternalMessageDTO.class);
                internalSubscribe(dto);
            } catch (Exception e) {
                log.error("Error processing internal message {}", e);
            }
        });
        log.info("Registered internal message listener");
        consumerMap.put(INTERNAL_MESSAGE_TOPIC_PREFIX, consumer);
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
