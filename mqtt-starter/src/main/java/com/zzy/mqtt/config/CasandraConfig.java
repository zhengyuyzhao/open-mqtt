package com.zzy.mqtt.config;

import lombok.Data;

@Data
public class CasandraConfig {
    private String contactPoints;
    private String username;

    private String password;

    private int connectionMax = 50;
    private int port = 9042;
}
