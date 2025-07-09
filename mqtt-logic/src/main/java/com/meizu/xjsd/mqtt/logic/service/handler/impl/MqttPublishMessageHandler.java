package com.meizu.xjsd.mqtt.logic.service.handler.impl;

import com.meizu.xjsd.mqtt.logic.MqttLogic;
import com.meizu.xjsd.mqtt.logic.entity.IMqttPublishMessage;
import com.meizu.xjsd.mqtt.logic.service.handler.MessageHandler;
import com.meizu.xjsd.mqtt.logic.service.internal.CompositePublishService;
import com.meizu.xjsd.mqtt.logic.service.store.*;
import com.meizu.xjsd.mqtt.logic.service.transport.ITransport;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class MqttPublishMessageHandler implements MessageHandler<IMqttPublishMessage> {

    private final ISessionStoreService sessionStoreService;


    private final IRetainMessageStoreService retainMessageStoreService;


    private final CompositePublishService compositePublishService;


    @SneakyThrows
    @Override
    public void handle(IMqttPublishMessage event, ITransport transport) {
        // Handle the MQTT subscribe message here
        // This could involve processing the subscription, updating state, etc.
//        System.out.println("Handling MQTT Publish Message: " + event);

        MqttLogic.getPublishService().submit(() -> {
            // Call the inner handling logic
            handleInner(event, transport);
        });

    }

    @SneakyThrows
    private void handleInner(IMqttPublishMessage event, ITransport transport) {
        boolean isHandshakeOk = event.qosLevel().value() > 1 ? false : true;
        ClientPublishMessageStoreDTO clientPublishMessageStoreDTO =
                ClientPublishMessageStoreDTO.builder()
                        .clientId(transport.clientIdentifier())
                        .isHandshakeOk(isHandshakeOk)
                        .messageBytes(event.payload())
                        .mqttQoS(event.qosLevel().value())
                        .topic(event.topicName())
                        .messageId(event.messageId())
                        .createTime(System.currentTimeMillis())
                        .build();


        if (event.qosLevel().value() == 2 && !event.isRetain()) {
            compositePublishService.storeClientPublishMessage(clientPublishMessageStoreDTO);
        }

        if (event.qosLevel().value() < 2 && !event.isRetain()) {
            compositePublishService.storeServerPublishMessageAndSend(clientPublishMessageStoreDTO);
        }


        String clientId = transport.clientIdentifier();

        // publish 延长session失效时间
        if (sessionStoreService.containsKey(clientId)) {
            SessionStoreDTO sessionStoreDTO = sessionStoreService.get(clientId);
            sessionStoreService.expire(clientId, sessionStoreDTO.getExpire());
        }

        if (event.isRetain()) {
            byte[] messageBytes = event.payload();
            retainMessageStoreService.put(event.topicName(), RetainMessageStoreDTO.builder()
                    .topic(event.topicName())
                    .messageBytes(messageBytes)
                    .mqttQoS(event.qosLevel().value())
                    .build());
        }

        MqttLogic.getPublishProtocolService().submit(() -> {
            // Store the message in the server publish message store
            switch (event.qosLevel()) {

                case AT_LEAST_ONCE:
                    transport.publishAcknowledge(event.messageId());
                    break;

                case EXACTLY_ONCE:
                    transport.publishReceived(event.messageId());
                    break;
            }
        });

    }


}
