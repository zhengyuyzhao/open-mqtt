package com.zzy.mqtt.logic.service.handler.impl;

import com.zzy.mqtt.logic.MqttLogic;
import com.zzy.mqtt.logic.service.handler.MessageHandler;
import com.zzy.mqtt.logic.service.transport.ITransport;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class MqttPubrecHandler implements MessageHandler<Integer> {


    @SneakyThrows
    @Override
    public void handle(Integer messageId, ITransport transport) {
        // Handle the MQTT subscribe message here
        // This could involve processing the subscription, updating state, etc.
//        System.out.println("Handling MQTT Publish Message: " + event);

        MqttLogic.getPublishProtocolService().submit(() -> {
            handleInner(messageId, transport);
        });
    }

    private void handleInner(Integer event, ITransport transport) {
        log.debug("Handling MQTT Pubrec Message: {}", event);
        transport.publishRelease(event);
    }


}
