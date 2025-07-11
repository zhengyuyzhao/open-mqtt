package com.zzy.mqtt.broker.adapter;

import com.zzy.mqtt.logic.entity.IMqttPubAckMessage;
import com.zzy.mqtt.logic.entity.codes.IMqttReasonCode;
import io.netty.handler.codec.mqtt.MqttProperties;
import io.vertx.mqtt.messages.MqttPubAckMessage;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class VertxMqttPubAckMessageAdp implements IMqttPubAckMessage {
    private MqttPubAckMessage mqttPubAckMessage;

    public static VertxMqttPubAckMessageAdp of(MqttPubAckMessage mqttPubAckMessage) {
        return new VertxMqttPubAckMessageAdp(mqttPubAckMessage);
    }

    @Override
    public int messageId() {
        return mqttPubAckMessage.messageId();
    }

    @Override
    public IMqttReasonCode code() {
        return VertxMqttPubAckReasonCodeAdp.of(mqttPubAckMessage.code());
    }

    @Override
    public MqttProperties properties() {
        return mqttPubAckMessage.properties();
    }
}
