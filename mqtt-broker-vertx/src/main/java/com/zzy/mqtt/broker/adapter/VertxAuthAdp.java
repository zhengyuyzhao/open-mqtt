package com.zzy.mqtt.broker.adapter;

import com.zzy.mqtt.logic.entity.IMqttAuth;
import io.vertx.mqtt.MqttAuth;

public class VertxAuthAdp implements IMqttAuth {
    public static IMqttAuth of(MqttAuth mqttAuth) {
        if (mqttAuth == null) {
            return null;
        }
        VertxAuthAdp adp = new VertxAuthAdp();
        adp.mqttAuth = mqttAuth;
        return adp;
    }

    private MqttAuth mqttAuth;
    @Override
    public String getUsername() {
        return mqttAuth.getUsername();
    }

    @Override
    public String getPassword() {
        return mqttAuth.getPassword();
    }
}
