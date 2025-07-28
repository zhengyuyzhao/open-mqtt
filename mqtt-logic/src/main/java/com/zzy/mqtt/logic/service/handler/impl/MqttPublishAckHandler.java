package com.zzy.mqtt.logic.service.handler.impl;

import com.zzy.mqtt.logic.MqttLogic;
import com.zzy.mqtt.logic.entity.IMqttPubAckMessage;
import com.zzy.mqtt.logic.service.handler.MessageHandler;
import com.zzy.mqtt.logic.service.internal.CompositeStoreService;
import com.zzy.mqtt.logic.service.transport.ITransport;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class MqttPublishAckHandler implements MessageHandler<IMqttPubAckMessage> {
    private final CompositeStoreService compositeStoreService;


    @SneakyThrows
    @Override
    public void handle(IMqttPubAckMessage event, ITransport transport) {
        // Handle the MQTT subscribe message here
        // This could involve processing the subscription, updating state, etc.
//        System.out.println("Handling MQTT Publish Message: " + event);

        MqttLogic.getProtocolService().submit(() -> {
            handleInner(event, transport);
        });
    }

    @SneakyThrows
    private void handleInner(IMqttPubAckMessage event, ITransport transport) {
        log.debug("Handling MQTT PubAck Message: {}", event.messageId());
        compositeStoreService.removeServerPublishStore(
                transport.clientIdentifier(),
                event.messageId()
        ).get();
    }


}
