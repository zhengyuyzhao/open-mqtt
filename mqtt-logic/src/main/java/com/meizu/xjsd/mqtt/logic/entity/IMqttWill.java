package com.meizu.xjsd.mqtt.logic.entity;

import io.netty.handler.codec.mqtt.MqttProperties;

public interface IMqttWill {

    String getWillTopic();

    byte[] getWillMessage();

    byte[] getWillMessageBytes();

    int getWillQos();

    boolean isWillRetain();

    MqttProperties getWillProperties();
}
