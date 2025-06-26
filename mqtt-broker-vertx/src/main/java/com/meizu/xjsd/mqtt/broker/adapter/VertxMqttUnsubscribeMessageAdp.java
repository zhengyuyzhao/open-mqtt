package com.meizu.xjsd.mqtt.broker.adapter;

import com.meizu.xjsd.mqtt.logic.entity.IMqttUnsubscribeMessage;
import io.netty.handler.codec.mqtt.MqttProperties;
import io.vertx.mqtt.messages.MqttUnsubscribeMessage;
import lombok.AllArgsConstructor;

import java.util.List;

@AllArgsConstructor
public class VertxMqttUnsubscribeMessageAdp implements IMqttUnsubscribeMessage {

    private MqttUnsubscribeMessage mqttUnsubscribeMessage;

    public static VertxMqttUnsubscribeMessageAdp of(MqttUnsubscribeMessage mqttUnsubscribeMessage) {
        return new VertxMqttUnsubscribeMessageAdp(mqttUnsubscribeMessage);
    }

    @Override
    public int messageId() {
        return mqttUnsubscribeMessage.messageId();
    }

    @Override
    public List<String> topics() {
        return mqttUnsubscribeMessage.topics();
    }

    @Override
    public MqttProperties properties() {
        return mqttUnsubscribeMessage.properties();
    }
}
