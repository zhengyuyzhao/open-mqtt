package com.meizu.xjsd.mqtt.logic.service.handler.impl;

import com.meizu.xjsd.mqtt.logic.MqttLogic;
import com.meizu.xjsd.mqtt.logic.service.handler.MessageHandler;
import com.meizu.xjsd.mqtt.logic.service.transport.ITransport;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

@RequiredArgsConstructor
public class MqttPubrelHandler implements MessageHandler<Integer> {


    @SneakyThrows
    @Override
    public void handle(Integer messageId, ITransport transport) {
        // Handle the MQTT subscribe message here
        // This could involve processing the subscription, updating state, etc.
//        System.out.println("Handling MQTT Publish Message: " + event);

        MqttLogic.getExecutorService().submit(() -> {
            handleInner(messageId, transport);
        });
    }

    private void handleInner(Integer event, ITransport transport) {
        transport.publishComplete(event);
    }


}
