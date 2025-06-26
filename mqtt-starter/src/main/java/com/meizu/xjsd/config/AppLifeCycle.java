package com.meizu.xjsd.config;

import com.meizu.xjsd.mqtt.logic.MqttBroker;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AppLifeCycle implements ApplicationRunner, DisposableBean {

    @Resource
    private MqttBroker mqttBroker;

    @Override
    public void destroy() throws Exception {
        mqttBroker.stop();
        // Clean up resources or configurations here
        log.info("Application is shutting down.");
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        mqttBroker.start();
    }
}
