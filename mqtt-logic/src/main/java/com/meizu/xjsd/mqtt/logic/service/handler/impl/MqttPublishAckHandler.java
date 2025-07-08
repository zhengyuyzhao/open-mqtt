package com.meizu.xjsd.mqtt.logic.service.handler.impl;

import com.meizu.xjsd.mqtt.logic.MqttLogic;
import com.meizu.xjsd.mqtt.logic.entity.IMqttPubAckMessage;
import com.meizu.xjsd.mqtt.logic.service.handler.MessageHandler;
import com.meizu.xjsd.mqtt.logic.service.store.IServerPublishMessageStoreService;
import com.meizu.xjsd.mqtt.logic.service.transport.ITransport;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class MqttPublishAckHandler implements MessageHandler<IMqttPubAckMessage> {
    private final IServerPublishMessageStoreService serverPublishMessageStoreService;


    @SneakyThrows
    @Override
    public void handle(IMqttPubAckMessage event, ITransport transport) {
        // Handle the MQTT subscribe message here
        // This could involve processing the subscription, updating state, etc.
//        System.out.println("Handling MQTT Publish Message: " + event);

        MqttLogic.getPublishProtocolService().submit(() -> {
            handleInner(event, transport);
        });
    }

    private void handleInner(IMqttPubAckMessage event, ITransport transport) {
        log.debug("Handling MQTT PubAck Message: {}", event.messageId());
        MqttLogic.getPublishService().submit(() -> {
            // Remove the message from the server publish message store
            serverPublishMessageStoreService.remove(transport.clientIdentifier(), event.messageId());
        });
        serverPublishMessageStoreService.remove(transport.clientIdentifier(), event.messageId());
    }


}
