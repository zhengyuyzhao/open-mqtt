package com.meizu.xjsd.mqtt.logic.service.handler.impl;

import com.meizu.xjsd.mqtt.logic.MqttLogic;
import com.meizu.xjsd.mqtt.logic.entity.IMqttPublishMessage;
import com.meizu.xjsd.mqtt.logic.service.handler.MessageHandler;
import com.meizu.xjsd.mqtt.logic.service.internal.IInternalMessageService;
import com.meizu.xjsd.mqtt.logic.service.internal.InternalMessageDTO;
import com.meizu.xjsd.mqtt.logic.service.store.IRetainMessageStoreService;
import com.meizu.xjsd.mqtt.logic.service.store.ISessionStoreService;
import com.meizu.xjsd.mqtt.logic.service.store.RetainMessageStoreDTO;
import com.meizu.xjsd.mqtt.logic.service.store.SessionStoreDTO;
import com.meizu.xjsd.mqtt.logic.service.transport.ITransport;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class MqttPublishMessageHandler implements MessageHandler<IMqttPublishMessage> {

    private final ISessionStoreService sessionStoreService;

    private final IInternalMessageService internalCommunication;

    private final IRetainMessageStoreService retainMessageStoreService;


    @Override
    public void handle(IMqttPublishMessage event, ITransport transport) {
        // Handle the MQTT subscribe message here
        // This could involve processing the subscription, updating state, etc.
//        System.out.println("Handling MQTT Publish Message: " + event);

        MqttLogic.getExecutorService().execute(()->{
            try {
                // Call the inner handling logic
                handleInner(event, transport);
            } catch (Exception e) {
                // Handle any exceptions that occur during processing
                e.printStackTrace();
            }

        });

    }

    private void handleInner(IMqttPublishMessage event, ITransport transport) throws Exception {
        String clientId = transport.clientIdentifier();
        // publish 延长session失效时间
        if (sessionStoreService.containsKey(clientId)) {
            SessionStoreDTO sessionStoreDTO = sessionStoreService.get(clientId);
            sessionStoreService.expire(clientId, sessionStoreDTO.getExpire());
        }
        byte[] messageBytes = event.payload();
        InternalMessageDTO internalMessageDTO = InternalMessageDTO.builder().topic(event.topicName())
                .mqttQoS(event.qosLevel().value()).messageBytes(messageBytes)
                .dup(false).retain(false).clientId(clientId).build();
        internalCommunication.internalPublish(internalMessageDTO);

        if (event.isRetain()) {
            retainMessageStoreService.put(event.topicName(), RetainMessageStoreDTO.builder()
                    .topic(event.topicName())
                    .messageBytes(messageBytes)
                    .mqttQoS(event.qosLevel().value())
                    .build());
        }
    }


}
