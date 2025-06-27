package com.meizu.xjsd.config;

import com.meizu.xjsd.mqtt.logic.config.MqttLogicConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BrokerConfig {

    @Bean
    public MqttLogicConfig mqttLogicConfig() {
        MqttLogicConfig config = new MqttLogicConfig();
        // Set any necessary properties on the config object
        // For example: config.setBrokerId("your-broker-id");
        return config;
    }
}
