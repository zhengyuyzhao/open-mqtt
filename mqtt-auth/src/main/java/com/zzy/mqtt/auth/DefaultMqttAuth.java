package com.zzy.mqtt.auth;

import com.zzy.mqtt.logic.entity.IMqttAuth;
import com.zzy.mqtt.logic.service.auth.IAuthService;

public class DefaultMqttAuth implements IAuthService {


    @Override
    public boolean checkValid(IMqttAuth mqttAuth) {
        return true;
    }
}
