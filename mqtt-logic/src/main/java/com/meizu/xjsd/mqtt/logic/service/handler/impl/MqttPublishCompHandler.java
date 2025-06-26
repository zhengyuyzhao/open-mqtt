package com.meizu.xjsd.mqtt.logic.service.handler.impl;

import com.meizu.xjsd.mqtt.logic.MqttLogic;
import com.meizu.xjsd.mqtt.logic.entity.IMqttPubCompMessage;
import com.meizu.xjsd.mqtt.logic.service.handler.MessageHandler;
import com.meizu.xjsd.mqtt.logic.service.store.IDupPublishMessageStoreService;
import com.meizu.xjsd.mqtt.logic.service.transport.ITransport;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class MqttPublishCompHandler implements MessageHandler<IMqttPubCompMessage> {
    private final IDupPublishMessageStoreService dupPublishMessageStoreService;


    @Override
    public void handle(IMqttPubCompMessage event, ITransport transport) {
        // Handle the MQTT subscribe message here
        // This could involve processing the subscription, updating state, etc.
//        System.out.println("Handling MQTT Publish Message: " + event);

        MqttLogic.getExecutorService().execute(()->{
            handleInner(event, transport);
        });

    }

    private void handleInner(IMqttPubCompMessage event, ITransport transport) {
        dupPublishMessageStoreService.remove(transport.clientIdentifier(), event.messageId());
    }


}
