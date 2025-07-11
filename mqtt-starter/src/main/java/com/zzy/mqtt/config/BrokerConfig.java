package com.zzy.mqtt.config;

import com.zzy.mqtt.logic.config.MqttLogicConfig;
import lombok.Data;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ToString
@ConfigurationProperties(prefix = "spring.mqtt")
public class BrokerConfig {
    private MqttLogicConfig broker;
}
