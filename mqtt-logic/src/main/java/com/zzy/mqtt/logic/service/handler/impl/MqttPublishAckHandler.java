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
            try {
                // Call the inner handling logic
                handleInner(event, transport);
            } catch (Exception e) {
                log.error("Error handling MQTT PubAck Message: {}", event, e);
                // Handle the exception appropriately, maybe send an error response or log it
            }
//            handleInner(event, transport);
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
