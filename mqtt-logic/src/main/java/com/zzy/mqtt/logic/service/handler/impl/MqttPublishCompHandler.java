package com.zzy.mqtt.logic.service.handler.impl;

import com.zzy.mqtt.logic.MqttLogic;
import com.zzy.mqtt.logic.entity.IMqttPubCompMessage;
import com.zzy.mqtt.logic.service.handler.MessageHandler;
import com.zzy.mqtt.logic.service.store.IServerPublishMessageStoreService;
import com.zzy.mqtt.logic.service.transport.ITransport;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class MqttPublishCompHandler implements MessageHandler<IMqttPubCompMessage> {
    private final IServerPublishMessageStoreService serverPublishMessageStoreService;


    @SneakyThrows
    @Override
    public void handle(IMqttPubCompMessage event, ITransport transport) {
        // Handle the MQTT subscribe message here
        // This could involve processing the subscription, updating state, etc.
//        System.out.println("Handling MQTT Publish Message: " + event);

        MqttLogic.getPublishProtocolService().submit(() -> {
            handleInner(event, transport);
        });

    }

    private void handleInner(IMqttPubCompMessage event, ITransport transport) {
        log.debug("Handling MQTT PubComp Message: {}", event.messageId());
        MqttLogic.getPublishService().submit(() -> {
            // Remove the message from the server publish message store
            serverPublishMessageStoreService.remove(transport.clientIdentifier(), event.messageId());
        });

    }


}
