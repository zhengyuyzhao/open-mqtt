package com.meizu.xjsd.config;

import com.meizu.xjsd.mqtt.auth.DefaultMqttAuth;
import com.meizu.xjsd.mqtt.broker.VertxMqttBroker;
import com.meizu.xjsd.mqtt.broker.cluster.VertxCluster;
import com.meizu.xjsd.mqtt.broker.cluster.VertxClusterInternalMessageService;
import com.meizu.xjsd.mqtt.logic.MqttBroker;
import com.meizu.xjsd.mqtt.logic.MqttLogic;
import com.meizu.xjsd.mqtt.logic.config.MqttLogicConfig;
import com.meizu.xjsd.mqtt.logic.service.internal.IInternalMessageService;
import com.meizu.xjsd.mqtt.logic.service.store.*;
import com.meizu.xjsd.mqtt.logic.service.transport.ITransportLocalStoreService;
import com.meizu.xjsd.mqtt.logic.service.transport.IClientStoreService;
import com.meizu.xjsd.mqtt.logic.service.transport.impl.DefaultTransportLocalStoreService;
import io.vertx.core.spi.cluster.ClusterManager;
import io.vertx.spi.cluster.ignite.IgniteClusterManager;
import jakarta.annotation.Resource;
import org.apache.ignite.Ignite;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

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
    IDupPublishMessageStoreService dupPublishMessageStoreService;

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
                transportLocalStoreService(), clientStoreService, subscribeStoreService, vertxCluster()); // Replace with actual implementation if needed
    }



    @Bean
    public MqttLogic mqttLogic() {
        return MqttLogic.builder()
                .mqttLogicConfig(brokerConfig.getBroker())
                .authService(new DefaultMqttAuth())
                .dupPublishMessageStoreService(dupPublishMessageStoreService)
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
