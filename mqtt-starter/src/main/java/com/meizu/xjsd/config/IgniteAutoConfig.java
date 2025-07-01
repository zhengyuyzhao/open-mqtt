/**
 * Copyright (c) 2018, Mr.Wang (recallcode@aliyun.com) All rights reserved.
 */

package com.meizu.xjsd.config;

import cn.hutool.core.util.StrUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.Ignition;
import org.apache.ignite.cache.CacheAtomicityMode;
import org.apache.ignite.cache.CacheMode;
import org.apache.ignite.cluster.ClusterNode;
import org.apache.ignite.cluster.ClusterState;
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
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.cache.expiry.AccessedExpiryPolicy;
import javax.cache.expiry.Duration;
import java.util.Arrays;
import java.util.Collection;

/**
 * 自动配置apache ignite
 */
@Slf4j
@Configuration
@EnableConfigurationProperties({IgniteProperties.class, BrokerConfig.class})
@SuppressWarnings("unchecked")
public class IgniteAutoConfig {

    @Resource
    private BrokerConfig brokerConfig;

    @Resource
    private IgniteProperties igniteProperties;

//    @Value("${spring.mqtt.broker.enable-multicast-group: false}")
//    private boolean enableMulticastGroup;
//
//    @Value("${spring.mqtt.broker.multicast-group: ff01::1}")
//    private String multicastGroup;
//
//    @Value("${spring.mqtt.broker.static-ip-addresses: [172.20.21.99:47500]}")
//    private String[] staticIpAddresses;

    private static final String NOT_PERSISTENCE_DATA_REGION = "not-persistence-data-region";
    private static final String PERSISTENCE_DATA_REGION = "persistence-data-region";


    @Bean
    public Ignite ignite() throws Exception {
        log.info("Starting Ignite with configuration: {}", igniteProperties);
        log.info("Broker configuration: {}", brokerConfig.getBroker());

        IgniteConfiguration igniteConfiguration = new IgniteConfiguration();
        // Ignite实例名称
        igniteConfiguration.setIgniteInstanceName(brokerConfig.getBroker().getBrokerId());
        // Ignite日志
        Logger logger = LoggerFactory.getLogger("org.apache.ignite");
        igniteConfiguration.setGridLogger(new Slf4jLogger(logger));
        igniteConfiguration.setSnapshotPath(igniteProperties.getSnapshotPath());
        igniteConfiguration.setWorkDirectory(igniteProperties.getWorkPath());
        igniteConfiguration.setMetricsLogFrequency(0);
        // 非持久化数据区域
        DataRegionConfiguration notPersistence = new DataRegionConfiguration().setPersistenceEnabled(false)
                .setInitialSize(igniteProperties.getNotPersistenceInitialSize() * 1024 * 1024)
                .setMaxSize(igniteProperties.getNotPersistenceMaxSize() * 1024 * 1024)
                .setName(NOT_PERSISTENCE_DATA_REGION);
        // 持久化数据区域
        DataRegionConfiguration persistence = new DataRegionConfiguration().setPersistenceEnabled(true)
                .setInitialSize(igniteProperties.getPersistenceInitialSize() * 1024 * 1024)
                .setMaxSize(igniteProperties.getPersistenceMaxSize() * 1024 * 1024)
                .setName(PERSISTENCE_DATA_REGION);
        DataStorageConfiguration dataStorageConfiguration = new DataStorageConfiguration()
                .setDefaultDataRegionConfiguration(notPersistence)
                .setDataRegionConfigurations(persistence)
                .setCdcWalPath(StrUtil.isNotBlank(igniteProperties.getPersistenceStorePath()) ? igniteProperties.getPersistenceStorePath() : null)
                .setWalArchivePath(StrUtil.isNotBlank(igniteProperties.getPersistenceStorePath()) ? igniteProperties.getPersistenceStorePath() : null)
                .setWalPath(StrUtil.isNotBlank(igniteProperties.getPersistenceStorePath()) ? igniteProperties.getPersistenceStorePath() : null)
                .setStoragePath(StrUtil.isNotBlank(igniteProperties.getPersistenceStorePath()) ? igniteProperties.getPersistenceStorePath() : null);
        igniteConfiguration.setDataStorageConfiguration(dataStorageConfiguration);
        // 集群, 基于组播或静态IP配置
        TcpDiscoverySpi tcpDiscoverySpi = new TcpDiscoverySpi();
        if (this.igniteProperties.isEnableMulticastGroup()) {
            TcpDiscoveryMulticastIpFinder tcpDiscoveryMulticastIpFinder = new TcpDiscoveryMulticastIpFinder();
            tcpDiscoveryMulticastIpFinder.setMulticastGroup(this.igniteProperties.getMulticastGroup());
            tcpDiscoverySpi.setIpFinder(tcpDiscoveryMulticastIpFinder);
        } else {
            TcpDiscoveryVmIpFinder tcpDiscoveryVmIpFinder = new TcpDiscoveryVmIpFinder();
            tcpDiscoveryVmIpFinder.setAddresses(Arrays.asList(this.igniteProperties.getStaticIpAddresses()));
            tcpDiscoverySpi.setIpFinder(tcpDiscoveryVmIpFinder);
        }
        igniteConfiguration.setDiscoverySpi(tcpDiscoverySpi);
        Ignite ignite = Ignition.start(igniteConfiguration);
        ignite.cluster().state(ClusterState.ACTIVE);
//        Collection<ClusterNode> dataNodes = ignite.cluster().forServers().nodes();
//        dataNodes.stream().forEach(node -> {
//            log.info("Ignite node: {} with ID: {}", node.addresses(), node.id());
//        });
        ignite.cluster().baselineAutoAdjustEnabled(true);
        ignite.cluster().baselineAutoAdjustTimeout(60000);
        // 设置新的基线拓扑
//        ignite.cluster().setBaselineTopology(dataNodes);



        return ignite;
    }

    @Bean
    public IgniteCache messageIdCache() throws Exception {
        CacheConfiguration cacheConfiguration = new CacheConfiguration()
                .setDataRegionName(PERSISTENCE_DATA_REGION)
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
                .setDataRegionName(PERSISTENCE_DATA_REGION)
                .setBackups(1)
                .setCacheMode(CacheMode.PARTITIONED).setAtomicityMode(CacheAtomicityMode.TRANSACTIONAL).setName("retainMessageCache");
        return ignite().getOrCreateCache(cacheConfiguration);
    }

    @Bean
    public IgniteCache subscribeCache() throws Exception {
        CacheConfiguration cacheConfiguration = new CacheConfiguration()
                .setDataRegionName(PERSISTENCE_DATA_REGION)
                .setReadFromBackup(true)
                .setCacheMode(CacheMode.REPLICATED)
                .setAtomicityMode(CacheAtomicityMode.TRANSACTIONAL)
                .setName("subscribeCache");
        return ignite().getOrCreateCache(cacheConfiguration);
    }

    @Bean
    public IgniteCache subscribeWildCardCache() throws Exception {
        CacheConfiguration cacheConfiguration = new CacheConfiguration()
                .setDataRegionName(PERSISTENCE_DATA_REGION)
                .setReadFromBackup(true)
                .setCacheMode(CacheMode.REPLICATED)
                .setAtomicityMode(CacheAtomicityMode.TRANSACTIONAL)
                .setName("subscribeWildCardCache");
        return ignite().getOrCreateCache(cacheConfiguration);
    }

    @Bean
    public IgniteCache sessionCache() throws Exception {
        CacheConfiguration cacheConfiguration = new CacheConfiguration()
                .setExpiryPolicyFactory(AccessedExpiryPolicy.factoryOf(Duration.ONE_DAY))
                .setDataRegionName(PERSISTENCE_DATA_REGION)
                .setBackups(1)
                .setCacheMode(CacheMode.PARTITIONED)
                .setAtomicityMode(CacheAtomicityMode.TRANSACTIONAL)
                .setName("sessionCache");
        return ignite().getOrCreateCache(cacheConfiguration);
    }

    @Bean
    public IgniteCache dupPublishMessageCache() throws Exception {
        CacheConfiguration cacheConfiguration = new CacheConfiguration()
                .setExpiryPolicyFactory(AccessedExpiryPolicy.factoryOf(Duration.ONE_DAY))
                .setDataRegionName(PERSISTENCE_DATA_REGION)
                .setBackups(1)
                .setCacheMode(CacheMode.PARTITIONED)
                .setAtomicityMode(CacheAtomicityMode.TRANSACTIONAL)
                .setName("dupPublishMessageCache");
        return ignite().getOrCreateCache(cacheConfiguration);
    }

    @Bean
    public IgniteCache transportCache() throws Exception {
        CacheConfiguration cacheConfiguration = new CacheConfiguration()
                .setExpiryPolicyFactory(AccessedExpiryPolicy.factoryOf(Duration.ONE_DAY))
                .setDataRegionName(NOT_PERSISTENCE_DATA_REGION)
                .setReadFromBackup(true)
                .setCacheMode(CacheMode.PARTITIONED)
                .setBackups(1)
//                .setAtomicityMode(CacheAtomicityMode.TRANSACTIONAL)
                .setName("transportCache");
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
