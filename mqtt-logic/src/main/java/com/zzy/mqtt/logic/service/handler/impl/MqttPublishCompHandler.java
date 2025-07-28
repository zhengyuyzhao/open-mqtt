package com.zzy.mqtt.logic.service.handler.impl;

import com.zzy.mqtt.logic.MqttLogic;
import com.zzy.mqtt.logic.entity.IMqttPubCompMessage;
import com.zzy.mqtt.logic.service.handler.MessageHandler;
import com.zzy.mqtt.logic.service.internal.CompositeStoreService;
import com.zzy.mqtt.logic.service.transport.ITransport;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class MqttPublishCompHandler implements MessageHandler<IMqttPubCompMessage> {
    private final CompositeStoreService compositeStoreService;


    @SneakyThrows
    @Override
    public void handle(IMqttPubCompMessage event, ITransport transport) {
        // Handle the MQTT subscribe message here
        // This could involve processing the subscription, updating state, etc.
//        System.out.println("Handling MQTT Publish Message: " + event);

        MqttLogic.getProtocolService().submit(() -> {
            handleInner(event, transport);
        });

    }

    @SneakyThrows
    private void handleInner(IMqttPubCompMessage event, ITransport transport) {
        log.debug("Handling MQTT PubComp Message: {}", event.messageId());
        compositeStoreService.removeServerPublishStore(
                transport.clientIdentifier(),
                event.messageId()
        ).get();

    }


}
