package com.meizu.xjsd.mqtt.logic.config;

import lombok.Data;

@Data
public class MqttLogicConfig {
    private int dupMessageRetryThreadPoolSize = 10;
    private int dupMessageRetryDelay = 10;
    private int dupMessageRetryInitialDelay = 10;
    private int dupMessageRetryMaxTimes = 3;
    private int serverPort = 1883;
}
