package com.zzy.mqtt.logic.service.handler.impl;

import com.zzy.mqtt.logic.MqttLogic;
import com.zzy.mqtt.logic.service.handler.MessageHandler;
import com.zzy.mqtt.logic.service.internal.CompositeStoreService;
import com.zzy.mqtt.logic.service.transport.ITransport;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class MqttPubrelHandler implements MessageHandler<Integer> {
    private final CompositeStoreService compositeStoreService;


    @SneakyThrows
    @Override
    public void handle(Integer messageId, ITransport transport) {
        // Handle the MQTT subscribe message here
        // This could involve processing the subscription, updating state, etc.
//        System.out.println("Handling MQTT Publish Message: " + event);

        MqttLogic.getProtocolService().submit(() -> {
            try {
                // Call the inner handling logic
                handleInner(messageId, transport);
            } catch (Exception e) {
                log.error("Error handling MQTT Pubrel Message: {}", messageId, e);
                // Handle the exception appropriately, maybe send an error response or log it
            }
//            handleInner(messageId, transport);
        });
    }

    @SneakyThrows
    private void handleInner(Integer event, ITransport transport) {
        log.debug("Handling MQTT Pubrel Message: {}", event);
        transport.publishComplete(event);

        compositeStoreService.storeServerPublishMessageAndSendByClientPublishMessage
                (transport.clientIdentifier(), event).get();

    }


}
