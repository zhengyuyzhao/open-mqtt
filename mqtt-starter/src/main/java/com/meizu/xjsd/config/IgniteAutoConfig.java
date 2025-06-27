/**
 * Copyright (c) 2018, Mr.Wang (recallcode@aliyun.com) All rights reserved.
 */

package com.meizu.xjsd.config;

import cn.hutool.core.util.StrUtil;
import com.meizu.xjsd.mqtt.logic.config.MqttLogicConfig;
import jakarta.annotation.Resource;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.Ignition;
import org.apache.ignite.cache.CacheAtomicityMode;
import org.apache.ignite.cache.CacheMode;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.DataRegionConfiguration;
import org.apache.ignite.configuration.DataStorageConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.logger.slf4j.Slf4jLogger;
import org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi;
import org.apache.ignite.spi.discovery.tcp.ipfinder.multicast.TcpDiscoveryMulticastIpFinder;
import org.apache.ignite.spi.discovery.tcp.ipfinder.vm.TcpDiscoveryVmIpFinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.cache.expiry.AccessedExpiryPolicy;
import javax.cache.expiry.Duration;
import java.util.Arrays;

/**
 * 自动配置apache ignite
 */
@Configuration
@EnableConfigurationProperties(IgniteProperties.class)
@SuppressWarnings("unchecked")
public class IgniteAutoConfig {

    @Resource
    private MqttLogicConfig mqttLogicConfig;

    @Value("${spring.mqtt.broker.enable-multicast-group: false}")
    private boolean enableMulticastGroup;

    @Value("${spring.mqtt.broker.multicast-group: ff01::1}")
    private String multicastGroup;

    @Value("${spring.mqtt.broker.static-ip-addresses: [172.20.21.99:47500]}")
    private String[] staticIpAddresses;

    @Bean
    public IgniteProperties igniteProperties() {
        return new IgniteProperties();
    }

    @Bean
    public Ignite ignite() throws Exception {
        IgniteConfiguration igniteConfiguration = new IgniteConfiguration();
        // Ignite实例名称
        igniteConfiguration.setIgniteInstanceName(mqttLogicConfig.getBrokerId());
        // Ignite日志
        Logger logger = LoggerFactory.getLogger("org.apache.ignite");
        igniteConfiguration.setGridLogger(new Slf4jLogger(logger));
        igniteConfiguration.setSnapshotPath(igniteProperties().getSnapshotPath());
        igniteConfiguration.setWorkDirectory(igniteProperties().getWorkPath());
        // 非持久化数据区域
        DataRegionConfiguration notPersistence = new DataRegionConfiguration().setPersistenceEnabled(false)
                .setInitialSize(igniteProperties().getNotPersistenceInitialSize() * 1024 * 1024)
                .setMaxSize(igniteProperties().getNotPersistenceMaxSize() * 1024 * 1024).setName("not-persistence-data-region");
        // 持久化数据区域
        DataRegionConfiguration persistence = new DataRegionConfiguration().setPersistenceEnabled(true)
                .setInitialSize(igniteProperties().getPersistenceInitialSize() * 1024 * 1024)
                .setMaxSize(igniteProperties().getPersistenceMaxSize() * 1024 * 1024).setName("persistence-data-region");
        DataStorageConfiguration dataStorageConfiguration = new DataStorageConfiguration()
                .setDefaultDataRegionConfiguration(notPersistence)
                .setDataRegionConfigurations(persistence)
                .setCdcWalPath(StrUtil.isNotBlank(igniteProperties().getPersistenceStorePath()) ? igniteProperties().getPersistenceStorePath() : null)
                .setWalArchivePath(StrUtil.isNotBlank(igniteProperties().getPersistenceStorePath()) ? igniteProperties().getPersistenceStorePath() : null)
                .setWalPath(StrUtil.isNotBlank(igniteProperties().getPersistenceStorePath()) ? igniteProperties().getPersistenceStorePath() : null)
                .setStoragePath(StrUtil.isNotBlank(igniteProperties().getPersistenceStorePath()) ? igniteProperties().getPersistenceStorePath() : null);
        igniteConfiguration.setDataStorageConfiguration(dataStorageConfiguration);
        // 集群, 基于组播或静态IP配置
        TcpDiscoverySpi tcpDiscoverySpi = new TcpDiscoverySpi();
        if (this.enableMulticastGroup) {
            TcpDiscoveryMulticastIpFinder tcpDiscoveryMulticastIpFinder = new TcpDiscoveryMulticastIpFinder();
            tcpDiscoveryMulticastIpFinder.setMulticastGroup(multicastGroup);
            tcpDiscoverySpi.setIpFinder(tcpDiscoveryMulticastIpFinder);
        } else {
            TcpDiscoveryVmIpFinder tcpDiscoveryVmIpFinder = new TcpDiscoveryVmIpFinder();
            tcpDiscoveryVmIpFinder.setAddresses(Arrays.asList(staticIpAddresses));
            tcpDiscoverySpi.setIpFinder(tcpDiscoveryVmIpFinder);
        }
        igniteConfiguration.setDiscoverySpi(tcpDiscoverySpi);
        Ignite ignite = Ignition.start(igniteConfiguration);
        ignite.cluster().active(true);
        return ignite;
    }

    @Bean
    public IgniteCache messageIdCache() throws Exception {
        CacheConfiguration cacheConfiguration = new CacheConfiguration()
                .setDataRegionName("not-persistence-data-region")
                .setCacheMode(CacheMode.PARTITIONED)
                .setBackups(1)
                .setAtomicityMode(CacheAtomicityMode.TRANSACTIONAL)
                .setName("messageIdCache");
        return ignite().getOrCreateCache(cacheConfiguration);
    }

    @Bean
    public IgniteCache retainMessageCache() throws Exception {
        CacheConfiguration cacheConfiguration = new CacheConfiguration()
                .setExpiryPolicyFactory(AccessedExpiryPolicy.factoryOf(Duration.ONE_DAY))
                .setDataRegionName("persistence-data-region")
                .setBackups(1)
                .setCacheMode(CacheMode.PARTITIONED).setAtomicityMode(CacheAtomicityMode.TRANSACTIONAL).setName("retainMessageCache");
        return ignite().getOrCreateCache(cacheConfiguration);
    }

    @Bean
    public IgniteCache subscribeCache() throws Exception {
        CacheConfiguration cacheConfiguration = new CacheConfiguration()
                .setDataRegionName("persistence-data-region")
                .setReadFromBackup(true)
                .setCacheMode(CacheMode.REPLICATED).setAtomicityMode(CacheAtomicityMode.TRANSACTIONAL).setName("subscribeCache");
        return ignite().getOrCreateCache(cacheConfiguration);
    }

    @Bean
    public IgniteCache subscribeWildCardCache() throws Exception {
        CacheConfiguration cacheConfiguration = new CacheConfiguration()
                .setDataRegionName("persistence-data-region")
                .setReadFromBackup(true)
                .setCacheMode(CacheMode.REPLICATED).setAtomicityMode(CacheAtomicityMode.TRANSACTIONAL).setName("subscribeWildCardCache");
        return ignite().getOrCreateCache(cacheConfiguration);
    }

    @Bean
    public IgniteCache sessionCache() throws Exception {
        CacheConfiguration cacheConfiguration = new CacheConfiguration()
                .setExpiryPolicyFactory(AccessedExpiryPolicy.factoryOf(Duration.ONE_DAY))
                .setDataRegionName("persistence-data-region")
                .setBackups(1)
                .setCacheMode(CacheMode.PARTITIONED).setAtomicityMode(CacheAtomicityMode.TRANSACTIONAL).setName("sessionCache");
        return ignite().getOrCreateCache(cacheConfiguration);
    }

    @Bean
    public IgniteCache dupPublishMessageCache() throws Exception {
        CacheConfiguration cacheConfiguration = new CacheConfiguration()
                .setExpiryPolicyFactory(AccessedExpiryPolicy.factoryOf(Duration.ONE_DAY))
                .setDataRegionName("persistence-data-region")
                .setBackups(1)
                .setCacheMode(CacheMode.PARTITIONED).setAtomicityMode(CacheAtomicityMode.TRANSACTIONAL).setName("dupPublishMessageCache");
        return ignite().getOrCreateCache(cacheConfiguration);
    }
//
//	@Bean
//	public IgniteCache dupPubRelMessageCache() throws Exception {
//		CacheConfiguration cacheConfiguration = new CacheConfiguration().setDataRegionName("persistence-data-region")
//			.setCacheMode(CacheMode.PARTITIONED).setName("dupPubRelMessageCache");
//		return ignite().getOrCreateCache(cacheConfiguration);
//	}

//	@Bean
//	public IgniteMessaging igniteMessaging() throws Exception {
//		return ignite().message(ignite().cluster().forRemotes());
//	}

}
