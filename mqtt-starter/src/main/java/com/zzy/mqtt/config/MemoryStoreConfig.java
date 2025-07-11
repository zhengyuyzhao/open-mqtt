//package com.meizu.xjsd.config;
//
//import com.meizu.xjsd.mqtt.logic.service.store.*;
//import com.meizu.xjsd.mqtt.store.memory.*;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
//@Configuration
//public class MemoryStoreConfig {
//
//
//    @Bean
//    public IDupPublishMessageStoreService dupPublishMessageStoreService() {
//        // Assuming you have a concrete implementation of IDupPublishMessageStoreService
//        return new MemDupPublishMessageStoreService();
//    }
//
//    @Bean
//    public IMessageIdService messageIdService() {
//        // Assuming you have a concrete implementation of IMessageIdService
//        return new MemMessageIdService();
//    }
//
//    @Bean
//    public IRetainMessageStoreService retainMessageStoreService() {
//        // Assuming you have a concrete implementation of IRetainMessageStoreService
//        return new MemRetainMessageStoreService();
//    }
//
//    @Bean
//    public ISessionStoreService sessionStoreService() {
//        // Assuming you have a concrete implementation of ISessionStoreService
//        return new MemSessionStoreService();
//    }
//
//    @Bean
//    public ISubscribeStoreService subscribeStoreService() {
//        // Assuming you have a concrete implementation of ISubscribeStoreService
//        return new MemSubscribeStoreService();
//    }
//
//
//}
