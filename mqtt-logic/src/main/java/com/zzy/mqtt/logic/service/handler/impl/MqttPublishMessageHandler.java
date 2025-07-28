package com.zzy.mqtt.logic.service.handler.impl;

import com.zzy.mqtt.logic.MqttLogic;
import com.zzy.mqtt.logic.entity.IMqttPublishMessage;
import com.zzy.mqtt.logic.entity.codes.MqttPubRecRC;
import com.zzy.mqtt.logic.service.handler.MessageHandler;
import com.zzy.mqtt.logic.service.internal.CompositeStoreService;
import com.zzy.mqtt.logic.service.store.ClientPublishMessageStoreDTO;
import com.zzy.mqtt.logic.service.store.RetainMessageStoreDTO;
import com.zzy.mqtt.logic.service.transport.ITransport;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import static com.zzy.mqtt.logic.entity.codes.MqttPubAckRC.UNSPECIFIED_ERROR;

@Slf4j
@RequiredArgsConstructor
public class MqttPublishMessageHandler implements MessageHandler<IMqttPublishMessage> {


    private final CompositeStoreService compositeStoreService;


    @SneakyThrows
    @Override
    public void handle(IMqttPublishMessage event, ITransport transport) {
        // Handle the MQTT subscribe message here
        // This could involve processing the subscription, updating state, etc.
//        System.out.println("Handling MQTT Publish Message: " + event);
        try {
            MqttLogic.getProtocolService().submit(() -> {
                // Call the inner handling logic
                handleInner(event, transport);
            });
        } catch (Exception e) {
            log.error("Error handling MQTT Publish Message: {}", event);
            // Handle the exception appropriately, maybe send an error response or log it
            switch (event.qosLevel()) {
                case AT_MOST_ONCE:
                    // For QoS 0, we don't need to send any response
                    break;
                case AT_LEAST_ONCE:
                    // For QoS 1, we send a PUBACK
                    transport.publishAcknowledge(event.messageId(),
                            UNSPECIFIED_ERROR, null);
                    break;
                case EXACTLY_ONCE:
                    // For QoS 2, we send a PUBREC
                    transport.publishReceived(event.messageId(), MqttPubRecRC.UNSPECIFIED_ERROR, null);
                    break;
            }
        }

    }

    @SneakyThrows
    private void handleInner(IMqttPublishMessage event, ITransport transport) {
        boolean isHandshakeOk = event.qosLevel().value() > 1 ? false : true;
        ClientPublishMessageStoreDTO clientPublishMessageStoreDTO =
                ClientPublishMessageStoreDTO.builder()
                        .clientId(transport.clientIdentifier())
                        .isHandshakeOk(isHandshakeOk)
                        .messageBytes(event.payload())
                        .mqttQoS(event.qosLevel().value())
                        .topic(event.topicName())
                        .messageId(event.messageId())
                        .createTime(System.currentTimeMillis())
                        .build();


        if (event.qosLevel().value() == 2 && !event.isRetain()) {
            compositeStoreService.storeClientPublishMessage(clientPublishMessageStoreDTO).get();
        }

        if (event.qosLevel().value() < 2 && !event.isRetain()) {
            compositeStoreService.storeServerPublishMessageAndSend(clientPublishMessageStoreDTO).get();
        }


//        String clientId = transport.clientIdentifier();

        // publish 延长session失效时间
//        if (sessionStoreService.containsKey(clientId)) {
//            SessionStoreDTO sessionStoreDTO = sessionStoreService.get(clientId);
//            sessionStoreService.expire(clientId, sessionStoreDTO.getExpire());
//        }

        if (event.isRetain()) {
            byte[] messageBytes = event.payload();
            compositeStoreService.putRetainStore(
                    event.topicName(),
                    RetainMessageStoreDTO.builder()
                            .topic(event.topicName())
                            .messageBytes(messageBytes)
                            .mqttQoS(event.qosLevel().value())
                            .build()
            ).get();
        }


        // Store the message in the server publish message store
        switch (event.qosLevel()) {

            case AT_LEAST_ONCE:
                transport.publishAcknowledge(event.messageId());
                break;

            case EXACTLY_ONCE:
                transport.publishReceived(event.messageId());
                break;
        }


    }


}
