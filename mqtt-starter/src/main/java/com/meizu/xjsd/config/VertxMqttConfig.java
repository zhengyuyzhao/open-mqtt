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
import com.meizu.xjsd.mqtt.logic.service.transport.impl.DefaultTransportLocalStoreService;
import com.meizu.xjsd.mqtt.store.memory.*;
import io.vertx.core.spi.cluster.ClusterManager;
import io.vertx.spi.cluster.ignite.IgniteClusterManager;
import jakarta.annotation.Resource;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;

@Configuration
public class VertxMqttConfig {

    @Resource
    Ignite ignite;
    @Resource(name = "messageIdCache")
    IgniteCache<String, Long> messageIdCache;

    @Resource(name = "dupPublishMessageCache")
    IgniteCache<String, List<DupPublishMessageStoreDTO>>  dupPublishMessageCache;


    @Resource(name = "retainMessageCache")
    IgniteCache<String, RetainMessageStoreDTO> retainMessageCache;

    @Resource(name = "sessionCache")
    IgniteCache<String, SessionStoreDTO> sessionCache;

    @Resource(name = "subscribeCache")
    IgniteCache<String, Map<String, SubscribeStoreDTO>> subscribeCache;

    @Bean
    public IDupPublishMessageStoreService dupPublishMessageStoreService() {
        // Assuming you have a concrete implementation of IDupPublishMessageStoreService
        return new IgniteDupPublishMessageStoreService(
                ignite,
                dupPublishMessageCache
        );
    }

    @Bean
    public IMessageIdService messageIdService() {
        // Assuming you have a concrete implementation of IMessageIdService
        return new IgniteMessageIdService(
                ignite,
                messageIdCache
        );
    }

    @Bean
    public IRetainMessageStoreService retainMessageStoreService() {
        // Assuming you have a concrete implementation of IRetainMessageStoreService
        return new IgniteRetainMessageStoreService(
                retainMessageCache
        );
    }

    @Bean
    public ISessionStoreService sessionStoreService() {
        // Assuming you have a concrete implementation of ISessionStoreService
        return new IgniteSessionStoreService(
                sessionCache
        );
    }

    @Bean
    public ISubscribeStoreService subscribeStoreService() {
        // Assuming you have a concrete implementation of ISubscribeStoreService
        return new IgniteSubscribeStoreService(
                ignite,
                subscribeCache
        );
    }

    @Bean
    public ITransportLocalStoreService transportLocalStoreService() {
        // Assuming you have a concrete implementation of ITransportLocalStoreService
        return new DefaultTransportLocalStoreService();
    }

    @Bean
    public IInternalMessageService internalMessageService() {
        // Assuming you have a concrete implementation of IInternalMessageService
        return new VertxClusterInternalMessageService(transportLocalStoreService(), subscribeStoreService(), vertxCluster()); // Replace with actual implementation if needed
    }

    @Bean
    public MqttLogic mqttLogic() {
        return MqttLogic.builder()
                .mqttLogicConfig(new MqttLogicConfig())
                .authService(new DefaultMqttAuth())
                .dupPublishMessageStoreService(dupPublishMessageStoreService())
                .messageIdService(messageIdService())
                .retainMessageStoreService(retainMessageStoreService())
                .subscribeStoreService(subscribeStoreService())
                .sessionStoreService(sessionStoreService())
                .transportLocalStoreService(transportLocalStoreService())
                .internalMessageService(internalMessageService()) // Replace with actual implementation if needed
                .build();
    }

    @Bean
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
