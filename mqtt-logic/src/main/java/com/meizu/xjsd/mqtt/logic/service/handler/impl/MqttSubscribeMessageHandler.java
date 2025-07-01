package com.meizu.xjsd.mqtt.logic.service.handler.impl;

import cn.hutool.core.util.StrUtil;
import com.meizu.xjsd.mqtt.logic.MqttLogic;
import com.meizu.xjsd.mqtt.logic.entity.IMqttSubscribeMessage;
import com.meizu.xjsd.mqtt.logic.entity.codes.MqttSubAckRC;
import com.meizu.xjsd.mqtt.logic.service.handler.MessageHandler;
import com.meizu.xjsd.mqtt.logic.service.store.*;
import com.meizu.xjsd.mqtt.logic.service.transport.ITransport;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.handler.codec.mqtt.MqttTopicSubscription;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Slf4j
public class MqttSubscribeMessageHandler implements MessageHandler<IMqttSubscribeMessage> {

    private final ISubscribeStoreService subscribeStoreService;

    private final IRetainMessageStoreService retainMessageStoreService;

    private final IMessageIdService messageIdService;


    @SneakyThrows
    @Override
    public void handle(IMqttSubscribeMessage event, ITransport transport) {
        // Handle the MQTT subscribe message here
        // This could involve processing the subscription, updating state, etc.
//        System.out.println("Handling MQTT Subscribe Message: " + event);
        log.info("Handling MQTT Subscribe Message: {}", event);
        MqttLogic.getExecutorService().submit(() -> this.handleSubscribe(event, transport));

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
                    subscribeStoreService.put(topicFilter, subscribeStoreDTO);
                    mqttQoSList.add(mqttQoS);
                    log.info("SUBSCRIBE - clientId: {}, topFilter: {}, QoS: {}", clientId, topicFilter, mqttQoS.value());
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

    private void sendRetainMessage(ITransport transport, String topicFilter, MqttQoS mqttQoS) {
        List<RetainMessageStoreDTO> retainMessageStoreDTOS = retainMessageStoreService.search(topicFilter);
        retainMessageStoreDTOS.forEach(retainMessageStoreDTO -> {
            MqttQoS respQoS = retainMessageStoreDTO.getMqttQoS() > mqttQoS.value() ? mqttQoS : MqttQoS.valueOf(retainMessageStoreDTO.getMqttQoS());
            transport.publish(retainMessageStoreDTO.getTopic(), retainMessageStoreDTO.getMessageBytes(),
                    respQoS, false, true, messageIdService.getNextMessageId(transport.clientIdentifier())
            );

        });
    }
}
