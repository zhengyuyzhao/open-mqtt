package com.zzy.mqtt.broker.adapter;

import com.zzy.mqtt.logic.entity.IMqttWill;
import io.netty.handler.codec.mqtt.MqttProperties;
import io.vertx.mqtt.MqttWill;

public class VertxMqttWillAdp implements IMqttWill {
    private MqttWill mqttWill;

    public static IMqttWill of(MqttWill mqttWill) {
        if (mqttWill == null) {
            return null;
        }
        VertxMqttWillAdp adp = new VertxMqttWillAdp();
        adp.mqttWill = mqttWill;
        return adp;
    }

    @Override
    public String getWillTopic() {
        return mqttWill.getWillTopic();
    }

    @Override
    public byte[] getWillMessage() {
        return mqttWill.getWillMessageBytes();
    }

    @Override
    public byte[] getWillMessageBytes() {
        return mqttWill.getWillMessageBytes();
    }

    @Override
    public int getWillQos() {
        return mqttWill.getWillQos();
    }

    @Override
    public boolean isWillRetain() {
        return mqttWill.isWillRetain();
    }

    @Override
    public MqttProperties getWillProperties() {
        return mqttWill.getWillProperties();
    }
}
