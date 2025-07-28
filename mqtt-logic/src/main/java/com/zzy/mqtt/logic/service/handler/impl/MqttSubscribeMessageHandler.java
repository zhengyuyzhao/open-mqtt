package com.zzy.mqtt.logic.service.handler.impl;

import cn.hutool.core.util.StrUtil;
import com.zzy.mqtt.logic.MqttLogic;
import com.zzy.mqtt.logic.entity.IMqttSubscribeMessage;
import com.zzy.mqtt.logic.entity.codes.MqttSubAckRC;
import com.zzy.mqtt.logic.service.handler.MessageHandler;
import com.zzy.mqtt.logic.service.internal.CompositeStoreService;
import com.zzy.mqtt.logic.service.store.RetainMessageStoreDTO;
import com.zzy.mqtt.logic.service.store.SubscribeStoreDTO;
import com.zzy.mqtt.logic.service.transport.ITransport;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.handler.codec.mqtt.MqttTopicSubscription;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

@RequiredArgsConstructor
@Slf4j
public class MqttSubscribeMessageHandler implements MessageHandler<IMqttSubscribeMessage> {

    private final CompositeStoreService compositeStoreService;


    @SneakyThrows
    @Override
    public void handle(IMqttSubscribeMessage event, ITransport transport) {
        // Handle the MQTT subscribe message here
        // This could involve processing the subscription, updating state, etc.
//        System.out.println("Handling MQTT Subscribe Message: " + event);
        log.debug("Handling MQTT Subscribe Message: {}", event);
        MqttLogic.getProtocolService().submit(() -> this.handleSubscribe(event, transport));

    }

    private void handleSubscribe(IMqttSubscribeMessage event, ITransport transport) {

        List<MqttTopicSubscription> topicSubscriptions = event.topicSubscriptions();
        if (this.validTopicFilter(topicSubscriptions)) {
            String clientId = transport.clientIdentifier();
            List<MqttQoS> mqttQoSList = new ArrayList();
            topicSubscriptions.forEach(topicSubscription -> {
                String topicFilter = topicSubscription.topicName();
                MqttQoS mqttQoS = topicSubscription.qualityOfService();
                SubscribeStoreDTO subscribeStoreDTO = new SubscribeStoreDTO(clientId, topicFilter, mqttQoS.value());
                compositeStoreService.putSubscribeStore(topicFilter, subscribeStoreDTO);
                mqttQoSList.add(mqttQoS);
                log.debug("SUBSCRIBE - clientId: {}, topFilter: {}, QoS: {}", clientId, topicFilter, mqttQoS.value());
            });
            // 发布保留消息
            topicSubscriptions.forEach(topicSubscription -> {
                String topicFilter = topicSubscription.topicName();
                MqttQoS mqttQoS = topicSubscription.qualityOfService();
                this.sendRetainMessage(transport, topicFilter, mqttQoS);
            });
            transport.subscribeAcknowledge(event.messageId(), mqttQoSList);
        } else {
            List<MqttSubAckRC> subAckRCS = topicSubscriptions.stream().map(
                    topicSubscription -> MqttSubAckRC.TOPIC_FILTER_INVALID
            ).toList();
            transport.subscribeAcknowledge(event.messageId(), subAckRCS, event.properties());
        }

    }

    private boolean validTopicFilter(List<MqttTopicSubscription> topicSubscriptions) {
        for (MqttTopicSubscription topicSubscription : topicSubscriptions) {
            String topicFilter = topicSubscription.topicName();
            // 以#或+符号开头的、以/符号结尾的订阅按非法订阅处理, 这里没有参考标准协议
            if (StrUtil.startWith(topicFilter, '+') || StrUtil.endWith(topicFilter, '/'))
                return false;
            if (StrUtil.contains(topicFilter, '#')) {
                // 如果出现多个#符号的订阅按非法订阅处理
                if (StrUtil.count(topicFilter, '#') > 1) return false;
            }
            if (StrUtil.contains(topicFilter, '+')) {
                //如果+符号和/+字符串出现的次数不等的情况按非法订阅处理
                if (StrUtil.count(topicFilter, '+') != StrUtil.count(topicFilter, "/+")) return false;
            }
        }
        return true;
    }

    @SneakyThrows
    private void sendRetainMessage(ITransport transport, String topicFilter, MqttQoS mqttQoS) {
        List<RetainMessageStoreDTO> retainMessageStoreDTOS =
                compositeStoreService.searchRetainStore(topicFilter).get();
        retainMessageStoreDTOS.forEach(retainMessageStoreDTO -> {
            MqttQoS respQoS = retainMessageStoreDTO.getMqttQoS() > mqttQoS.value() ? mqttQoS : MqttQoS.valueOf(retainMessageStoreDTO.getMqttQoS());
            Integer messageId = null;
            try {
                messageId = compositeStoreService.getNextMessageId(transport.clientIdentifier()).get();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            transport.publish(retainMessageStoreDTO.getTopic(), retainMessageStoreDTO.getMessageBytes(),
                    respQoS, false, true, messageId
            );

        });
    }
}
