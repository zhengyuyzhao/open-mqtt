package com.meizu.xjsd.mqtt.broker.adapter;

import com.meizu.xjsd.mqtt.logic.entity.codes.IMqttReasonCode;
import io.vertx.mqtt.messages.codes.MqttPubCompReasonCode;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class VertxMqttPubCompReasonCodeAdp implements IMqttReasonCode {

    private MqttPubCompReasonCode mqttPubCompReasonCode;

    public static VertxMqttPubCompReasonCodeAdp of(MqttPubCompReasonCode mqttPubCompReasonCode) {
        return new VertxMqttPubCompReasonCodeAdp(mqttPubCompReasonCode);
    }

    @Override
    public byte value() {
        return mqttPubCompReasonCode.value();
    }

}
