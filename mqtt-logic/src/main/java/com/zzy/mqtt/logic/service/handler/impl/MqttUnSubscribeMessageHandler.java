package com.zzy.mqtt.logic.service.handler.impl;

import com.zzy.mqtt.logic.MqttLogic;
import com.zzy.mqtt.logic.entity.IMqttUnsubscribeMessage;
import com.zzy.mqtt.logic.service.handler.MessageHandler;
import com.zzy.mqtt.logic.service.store.ISubscribeStoreService;
import com.zzy.mqtt.logic.service.transport.ITransport;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@RequiredArgsConstructor
@Slf4j
public class MqttUnSubscribeMessageHandler implements MessageHandler<IMqttUnsubscribeMessage> {
    private final ISubscribeStoreService subscribeStoreService;

    @SneakyThrows
    @Override
    public void handle(IMqttUnsubscribeMessage event, ITransport transport){
        // Handle the MQTT subscribe message here
        // This could involve processing the subscription, updating state, etc.
        MqttLogic.getExecutorService().submit(()->{
            handleInner(event, transport);
        });
    }

    private void handleInner(IMqttUnsubscribeMessage event, ITransport transport) {
        List<String> topicFilters = event.topics();
        String clientId = transport.clientIdentifier();
        topicFilters.forEach(topicFilter -> {
            subscribeStoreService.remove(topicFilter, clientId);
            log.debug("UNSUBSCRIBE - clientId: {}, topicFilter: {}", clientId, topicFilter);
        });
        transport.unsubscribeAcknowledge(event.messageId());
    }


}
