package com.zzy.mqtt.config;

import com.zzy.mqtt.auth.DefaultMqttAuth;
import com.zzy.mqtt.broker.VertxMqttBroker;
import com.zzy.mqtt.broker.cluster.VertxCluster;
import com.zzy.mqtt.broker.cluster.VertxClusterInternalMessageService;
import com.zzy.mqtt.logic.MqttBroker;
import com.zzy.mqtt.logic.MqttLogic;
import com.zzy.mqtt.logic.service.internal.IInternalMessageService;
import com.zzy.mqtt.logic.service.store.*;
import com.zzy.mqtt.logic.service.transport.IClientStoreService;
import com.zzy.mqtt.logic.service.transport.ITransportLocalStoreService;
import com.zzy.mqtt.logic.service.transport.impl.DefaultTransportLocalStoreService;
import io.micrometer.core.instrument.MeterRegistry;
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
    public VertxClusterInternalMessageService internalMessageService(VertxCluster vertxCluster) {
        // Assuming you have a concrete implementation of IInternalMessageService
        return new VertxClusterInternalMessageService(brokerConfig.getBroker().getBrokerId(),
                transportLocalStoreService(), clientStoreService, subscribeStoreService,
                serverPublishMessageStoreService,
                messageIdService,
                vertxCluster); // Replace with actual implementation if needed
    }


    @Bean
    public MqttLogic mqttLogic(VertxCluster vertxCluster) {
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
                .internalMessageService(internalMessageService(vertxCluster)) // Replace with actual implementation if needed
                .build();
    }

    @Bean
    @DependsOn({"ignite", "vertxNodeInfo", "vertxSubs"})
    public ClusterManager clusterManager() {
        // Assuming you have a concrete implementation of ClusterManager
        // This is a placeholder, replace with actual cluster manager initialization
        log.debug("Initializing IgniteClusterManager with Ignite instance: {}", ignite.name());
        return new IgniteClusterManager(ignite);
    }

    @Bean
    public VertxCluster vertxCluster(MeterRegistry meterRegistry) {
        return new VertxCluster(clusterManager(), meterRegistry);
    }

    @Bean
    public MqttBroker mqttBroker(VertxCluster vertxCluster) {
        return new VertxMqttBroker(mqttLogic(vertxCluster), vertxCluster);
    }

    @Bean
    public ExecutorsMetrics executorsMetrics(MeterRegistry meterRegistry,
                                             MqttLogic mqttLogic,
                                             VertxClusterInternalMessageService internalMessageService) {
        ExecutorsMetrics executorsMetrics = new ExecutorsMetrics(meterRegistry);
        executorsMetrics.registerExecutor("ProtocolService", MqttLogic.getProtocolService());
        executorsMetrics.registerExecutor("ConnectService", MqttLogic.getConnectService());
        executorsMetrics.registerExecutor("StoreService", MqttLogic.getStoreService());
        executorsMetrics.registerExecutor("internalMessageServiceStoreService", internalMessageService.getStoreService());

        return executorsMetrics;
    }

}
