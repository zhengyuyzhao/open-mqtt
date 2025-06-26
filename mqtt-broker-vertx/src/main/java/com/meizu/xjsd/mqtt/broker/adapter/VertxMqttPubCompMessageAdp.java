package com.meizu.xjsd.mqtt.broker.adapter;

import com.meizu.xjsd.mqtt.logic.entity.IMqttPubCompMessage;
import com.meizu.xjsd.mqtt.logic.entity.codes.IMqttReasonCode;
import io.netty.handler.codec.mqtt.MqttProperties;
import io.vertx.mqtt.messages.MqttPubCompMessage;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class VertxMqttPubCompMessageAdp implements IMqttPubCompMessage {

    private MqttPubCompMessage mqttPubCompMessage;

    public static VertxMqttPubCompMessageAdp of(MqttPubCompMessage mqttPubCompMessage) {
        return new VertxMqttPubCompMessageAdp(mqttPubCompMessage);
    }


    @Override
    public int messageId() {
        return mqttPubCompMessage.messageId();
    }

    @Override
    public IMqttReasonCode code() {
        return VertxMqttPubCompReasonCodeAdp.of(mqttPubCompMessage.code());
    }

    @Override
    public MqttProperties properties() {
        return mqttPubCompMessage.properties();
    }
}
