package com.meizu.xjsd.mqtt.broker.adapter;

import com.meizu.xjsd.mqtt.logic.entity.IMqttPublishMessage;
import io.netty.handler.codec.mqtt.MqttProperties;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.vertx.mqtt.messages.MqttPublishMessage;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class VertxMqttPublishMessageAdp implements IMqttPublishMessage {
    private MqttPublishMessage mqttPublishMessage;

    public static VertxMqttPublishMessageAdp of(MqttPublishMessage mqttPublishMessage) {
        return new VertxMqttPublishMessageAdp(mqttPublishMessage);
    }

    @Override
    public int messageId() {
        return mqttPublishMessage.messageId();
    }

    @Override
    public MqttQoS qosLevel() {
        return mqttPublishMessage.qosLevel();
    }

    @Override
    public boolean isDup() {
        return mqttPublishMessage.isDup();
    }

    @Override
    public boolean isRetain() {
        return mqttPublishMessage.isRetain();
    }

    @Override
    public String topicName() {
        return mqttPublishMessage.topicName();
    }

    @Override
    public byte[] payload() {
        return mqttPublishMessage.payload().getBytes();
    }


    @Override
    public MqttProperties properties() {
        return mqttPublishMessage.properties();
    }
}
