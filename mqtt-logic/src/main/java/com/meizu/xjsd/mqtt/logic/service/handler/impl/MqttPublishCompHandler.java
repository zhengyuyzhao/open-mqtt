package com.meizu.xjsd.mqtt.logic.service.handler.impl;

import com.meizu.xjsd.mqtt.logic.MqttLogic;
import com.meizu.xjsd.mqtt.logic.entity.IMqttPubCompMessage;
import com.meizu.xjsd.mqtt.logic.service.handler.MessageHandler;
import com.meizu.xjsd.mqtt.logic.service.store.IDupPublishMessageStoreService;
import com.meizu.xjsd.mqtt.logic.service.transport.ITransport;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

@RequiredArgsConstructor
public class MqttPublishCompHandler implements MessageHandler<IMqttPubCompMessage> {
    private final IDupPublishMessageStoreService dupPublishMessageStoreService;


    @SneakyThrows
    @Override
    public void handle(IMqttPubCompMessage event, ITransport transport) {
        // Handle the MQTT subscribe message here
        // This could involve processing the subscription, updating state, etc.
//        System.out.println("Handling MQTT Publish Message: " + event);

        MqttLogic.getExecutorService().submit(() -> {
            handleInner(event, transport);
        }).get();

    }

    private void handleInner(IMqttPubCompMessage event, ITransport transport) {
        dupPublishMessageStoreService.remove(transport.clientIdentifier(), event.messageId());
    }


}
