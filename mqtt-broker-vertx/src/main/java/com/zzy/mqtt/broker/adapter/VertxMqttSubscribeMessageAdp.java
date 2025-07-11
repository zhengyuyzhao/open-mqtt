package com.zzy.mqtt.broker.adapter;

import com.zzy.mqtt.logic.entity.IMqttSubscribeMessage;
import io.netty.handler.codec.mqtt.MqttProperties;
import io.netty.handler.codec.mqtt.MqttTopicSubscription;
import io.vertx.mqtt.messages.MqttSubscribeMessage;
import lombok.AllArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@AllArgsConstructor
public class VertxMqttSubscribeMessageAdp implements IMqttSubscribeMessage {

    private MqttSubscribeMessage mqttSubscribeMessage;

    public static VertxMqttSubscribeMessageAdp of(MqttSubscribeMessage mqttSubscribeMessage) {
        return new VertxMqttSubscribeMessageAdp(mqttSubscribeMessage);
    }

    @Override
    public int messageId() {
        return mqttSubscribeMessage.messageId();
    }

    @Override
    public List<MqttTopicSubscription> topicSubscriptions() {
        return Optional.ofNullable(mqttSubscribeMessage.topicSubscriptions())
                .orElse(new ArrayList<>()).stream().map(
                topicSubscription -> new MqttTopicSubscription(topicSubscription.topicName(),
                        topicSubscription.subscriptionOption())
        ).collect(Collectors.toList());
    }

    @Override
    public MqttProperties properties() {
        return mqttSubscribeMessage.properties();
    }
}
