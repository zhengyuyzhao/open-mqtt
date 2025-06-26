package com.meizu.xjsd.mqtt.logic.service.handler.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.meizu.xjsd.mqtt.logic.MqttLogic;
import com.meizu.xjsd.mqtt.logic.entity.IMqttPublishMessage;
import com.meizu.xjsd.mqtt.logic.service.handler.MessageHandler;
import com.meizu.xjsd.mqtt.logic.service.internal.IInternalMessageService;
import com.meizu.xjsd.mqtt.logic.service.internal.InternalMessageDTO;
import com.meizu.xjsd.mqtt.logic.service.store.*;
import com.meizu.xjsd.mqtt.logic.service.transport.ITransport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class MqttPublishMessageHandler implements MessageHandler<IMqttPublishMessage> {

    private final ISessionStoreService sessionStoreService;

    private final IInternalMessageService internalCommunication;

    private final IRetainMessageStoreService retainMessageStoreService;

    private final IDupPublishMessageStoreService dupPublishMessageStoreService;

    private final ISubscribeStoreService subscribeStoreService;


    @Override
    public void handle(IMqttPublishMessage event, ITransport transport) {
        // Handle the MQTT subscribe message here
        // This could involve processing the subscription, updating state, etc.
//        System.out.println("Handling MQTT Publish Message: " + event);

        MqttLogic.getExecutorService().execute(() -> {
            try {
                // Call the inner handling logic
                handleInner(event, transport);
            } catch (Exception e) {
                // Handle any exceptions that occur during processing
                e.printStackTrace();
            }

        });

    }

    private void handleInner(IMqttPublishMessage event, ITransport transport) throws Exception {
        String clientId = transport.clientIdentifier();
        // publish 延长session失效时间
        if (sessionStoreService.containsKey(clientId)) {
            SessionStoreDTO sessionStoreDTO = sessionStoreService.get(clientId);
            sessionStoreService.expire(clientId, sessionStoreDTO.getExpire());
        }
        byte[] messageBytes = event.payload();
        InternalMessageDTO internalMessageDTO = InternalMessageDTO.builder().topic(event.topicName())
                .mqttQoS(event.qosLevel().value()).messageBytes(messageBytes)
                .dup(false).retain(false)
                .clientId(clientId)
                .build();
        internalCommunication.internalPublish(internalMessageDTO);

        if (event.qosLevel().value() > 0) {
            storeDupMessage(event);
        }

        if (event.isRetain()) {
            retainMessageStoreService.put(event.topicName(), RetainMessageStoreDTO.builder()
                    .topic(event.topicName())
                    .messageBytes(messageBytes)
                    .mqttQoS(event.qosLevel().value())
                    .build());
        }
    }

    private void storeDupMessage(IMqttPublishMessage dto ) {
        MqttLogic.getExecutorService().execute(() -> {
            try {
                List<SubscribeStoreDTO> list = subscribeStoreService.search(dto.topicName());
                if (CollectionUtil.isEmpty(list)) {
                    // No subscribers for this topic, no need to store duplicate message
                    return;
                }
                // Store the duplicate message for the client
                for (SubscribeStoreDTO subscribeStoreDTO : list) {
                    dupPublishMessageStoreService.put(subscribeStoreDTO.getClientId(),
                            DupPublishMessageStoreDTO.builder()
                                    .messageId(dto.messageId())
                                    .topic(dto.topicName())
                                    .mqttQoS(dto.qosLevel().value())
                                    .messageBytes(dto.payload())
                                    .clientId(subscribeStoreDTO.getClientId())
                                    .build());
                    log.info("Stored duplicate message for clientId: {}, topic: {}, messageId: {}",
                            subscribeStoreDTO.getClientId(), dto.topicName(), dto.messageId());
                }


            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }


}
