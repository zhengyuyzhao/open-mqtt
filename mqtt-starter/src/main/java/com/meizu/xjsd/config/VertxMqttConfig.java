package com.meizu.xjsd.config;

import com.meizu.xjsd.mqtt.auth.DefaultMqttAuth;
import com.meizu.xjsd.mqtt.broker.VertxMqttBroker;
import com.meizu.xjsd.mqtt.broker.cluster.VertxCluster;
import com.meizu.xjsd.mqtt.broker.cluster.VertxClusterInternalMessageService;
import com.meizu.xjsd.mqtt.logic.MqttBroker;
import com.meizu.xjsd.mqtt.logic.MqttLogic;
import com.meizu.xjsd.mqtt.logic.service.internal.IInternalMessageService;
import com.meizu.xjsd.mqtt.logic.service.store.*;
import com.meizu.xjsd.mqtt.logic.service.transport.ITransportLocalStoreService;
import com.meizu.xjsd.mqtt.logic.service.transport.IClientStoreService;
import com.meizu.xjsd.mqtt.logic.service.transport.impl.DefaultTransportLocalStoreService;
import io.vertx.core.spi.cluster.ClusterManager;
import io.vertx.spi.cluster.ignite.IgniteClusterManager;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.ignite.Ignite;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

@Slf4j
@Configuration
public class VertxMqttConfig {

    @Resource
    Ignite ignite;

    @Resource
    BrokerConfig brokerConfig;

    @Resource
    ISubscribeStoreService subscribeStoreService;

    @Resource
    IRetainMessageStoreService retainMessageStoreService;

    @Resource
    IServerPublishMessageStoreService serverPublishMessageStoreService;

    @Resource
    IClientPublishMessageStoreService clientPublishMessageStoreService;

    @Resource
    ISessionStoreService sessionStoreService;

    @Resource
    IMessageIdService messageIdService;

    @Resource
    IClientStoreService clientStoreService;


    @Bean
    public ITransportLocalStoreService transportLocalStoreService() {
        // Assuming you have a concrete implementation of ITransportLocalStoreService
        return new DefaultTransportLocalStoreService();
    }

    @Bean
    public IInternalMessageService internalMessageService() {
        // Assuming you have a concrete implementation of IInternalMessageService
        return new VertxClusterInternalMessageService(brokerConfig.getBroker().getBrokerId(),
                transportLocalStoreService(), clientStoreService, subscribeStoreService,
                serverPublishMessageStoreService,
                messageIdService,
                vertxCluster()); // Replace with actual implementation if needed
    }



    @Bean
    public MqttLogic mqttLogic() {
        return MqttLogic.builder()
                .mqttLogicConfig(brokerConfig.getBroker())
                .authService(new DefaultMqttAuth())
                .serverPublishMessageStoreService(serverPublishMessageStoreService)
                .clientPublishMessageStoreService(clientPublishMessageStoreService)
                .messageIdService(messageIdService)
                .retainMessageStoreService(retainMessageStoreService)
                .subscribeStoreService(subscribeStoreService)
                .sessionStoreService(sessionStoreService)
                .transportLocalStoreService(transportLocalStoreService())
                .clientStoreService(clientStoreService)
                .internalMessageService(internalMessageService()) // Replace with actual implementation if needed
                .build();
    }

    @Bean
    @DependsOn({"ignite", "vertxNodeInfo", "vertxSubs"})
    public ClusterManager clusterManager() {
        // Assuming you have a concrete implementation of ClusterManager
        // This is a placeholder, replace with actual cluster manager initialization
        log.info("Initializing IgniteClusterManager with Ignite instance: {}", ignite.name());
        return new IgniteClusterManager(ignite);
    }

    @Bean
    public VertxCluster vertxCluster() {
        return new VertxCluster(clusterManager());
    }

    @Bean
    public MqttBroker mqttBroker() {
        return new VertxMqttBroker(mqttLogic(), vertxCluster());
    }

}
