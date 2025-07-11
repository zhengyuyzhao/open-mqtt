package com.zzy.mqtt.logic.entity;

import io.netty.handler.codec.mqtt.MqttProperties;

public class MqttWill implements IMqttWill {
    private String willTopic;
    private byte[] willMessage;
    private int willQos;
    private boolean willRetain;
    private MqttProperties willProperties;

    public static MqttWill of(IMqttWill iMqttWill) {
        MqttWill mqttWill = new MqttWill();
        mqttWill.willTopic = iMqttWill.getWillTopic();
        mqttWill.willMessage = iMqttWill.getWillMessage();
        mqttWill.willQos = iMqttWill.getWillQos();
        mqttWill.willRetain = iMqttWill.isWillRetain();
        mqttWill.willProperties = iMqttWill.getWillProperties();
        return mqttWill;
    }

    @Override
    public String getWillTopic() {
        return willTopic;
    }

    @Override
    public byte[] getWillMessage() {
        return willMessage;
    }

    @Override
    public byte[] getWillMessageBytes() {
        return willMessage;
    }

    @Override
    public int getWillQos() {
        return willQos;
    }

    @Override
    public boolean isWillRetain() {
        return willRetain;
    }

    @Override
    public MqttProperties getWillProperties() {
        return willProperties;
    }
}
