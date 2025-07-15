package com.zzy.mqtt.logic.config;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class MqttLogicConfig {
    private String brokerId = "default-broker";
    private int dupMessageRetryThreadPoolSize = 1;
    private int dupMessageRetryDelay = 2000;
    private int dupMessageRetryInitialDelay = 2000;
    private int dupMessageRetryMaxTimes = 3;
    private int serverPort = 1883;
    private int publishTps  = 500; // 每秒发布消息的TPS
}
