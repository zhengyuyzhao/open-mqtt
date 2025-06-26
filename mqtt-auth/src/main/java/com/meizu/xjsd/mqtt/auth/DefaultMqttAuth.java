package com.meizu.xjsd.mqtt.auth;

import com.meizu.xjsd.mqtt.logic.entity.IMqttAuth;
import com.meizu.xjsd.mqtt.logic.service.auth.IAuthService;

public class DefaultMqttAuth implements IAuthService {


    @Override
    public boolean checkValid(IMqttAuth mqttAuth) {
        return true;
    }
}
