package com.meizu.xjsd.mqtt.logic.config;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class MqttLogicConfig {
    private String brokerId = "default-broker";
    private int dupMessageRetryThreadPoolSize = 10;
    private int dupMessageRetryDelay = 200;
    private int dupMessageRetryInitialDelay = 200;
    private int dupMessageRetryMaxTimes = 3;
    private int serverPort = 1883;
}
