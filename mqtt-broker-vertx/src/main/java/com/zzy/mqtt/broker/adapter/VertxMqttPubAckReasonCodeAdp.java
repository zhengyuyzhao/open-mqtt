package com.zzy.mqtt.broker.adapter;

import com.zzy.mqtt.logic.entity.codes.IMqttReasonCode;
import io.vertx.mqtt.messages.codes.MqttPubAckReasonCode;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class VertxMqttPubAckReasonCodeAdp implements IMqttReasonCode {

    private MqttPubAckReasonCode mqttPubCompReasonCode;

    public static VertxMqttPubAckReasonCodeAdp of(MqttPubAckReasonCode mqttPubCompReasonCode) {
        return new VertxMqttPubAckReasonCodeAdp(mqttPubCompReasonCode);
    }

    @Override
    public byte value() {
        return mqttPubCompReasonCode.value();
    }

}
