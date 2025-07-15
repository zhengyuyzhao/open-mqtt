package com.zzy.mqtt.config;

import com.zzy.mqtt.logic.service.store.*;
import com.zzy.mqtt.logic.service.transport.IClientStoreService;
import com.zzy.mqtt.store.memory.*;
import jakarta.annotation.Resource;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteAtomicLong;
import org.apache.ignite.IgniteCache;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
public class IgniteStoreConfig {


    @Resource
    Ignite ignite;

    @Resource(name = "serverPublishMessageCache")
    IgniteCache<String, Map<Integer, ServerPublishMessageStoreDTO>> serverPublishMessageCache;

    @Resource(name = "clientPublishMessageCache")
    IgniteCache<String, Map<Integer, ClientPublishMessageStoreDTO>> clientPublishMessageCache;


    @Resource(name = "retainMessageCache")
    IgniteCache<String, RetainMessageStoreDTO> retainMessageCache;

    @Resource(name = "sessionCache")
    IgniteCache<String, SessionStoreDTO> sessionCache;

    @Resource(name = "subscribeCache")
    IgniteCache<String, Map<String, SubscribeStoreDTO>> subscribeCache;

    @Resource(name = "subscribeWildCardCache")
    IgniteCache<String, Map<String, SubscribeStoreDTO>> subscribeWildCardCache;

    @Resource(name = "transportCache")
    IgniteCache<String, String> transportCache;

    @Bean
    public IServerPublishMessageStoreService serverPublishMessageStoreService() {
        // Assuming you have a concrete implementation of IDupPublishMessageStoreService
        return new IgniteServerPublishMessageStoreService(
                ignite,
                serverPublishMessageCache
        );
    }

    @Bean
    public IClientPublishMessageStoreService clientPublishMessageStoreService() {
        // Assuming you have a concrete implementation of IDupPublishMessageStoreService
        return new IgniteClientPublishMessageStoreService(
                ignite,
                clientPublishMessageCache
        );
    }

    @Bean
    public IMessageIdService messageIdService() {
        // Assuming you have a concrete implementation of IMessageIdService
        return new IgniteMessageIdService(
                ignite
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
                subscribeWildCardCache,
                subscribeCache
        );
    }

    @Bean
    public IClientStoreService clientStoreService() {
        // Assuming you have a concrete implementation of ITransportLocalStoreService
        return new IgniteClientStoreService(
                transportCache
        );
    }


}
