/**
 * Copyright (c) 2018, Mr.Wang (recallcode@aliyun.com) All rights reserved.
 */

package com.zzy.mqtt.config;

import cn.hutool.core.util.StrUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.Ignition;
import org.apache.ignite.cache.CacheAtomicityMode;
import org.apache.ignite.cache.CacheMode;
import org.apache.ignite.cache.PartitionLossPolicy;
import org.apache.ignite.cluster.ClusterState;
import org.apache.ignite.configuration.*;
import org.apache.ignite.logger.slf4j.Slf4jLogger;
import org.apache.ignite.spi.communication.tcp.TcpCommunicationSpi;
import org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi;
import org.apache.ignite.spi.discovery.tcp.ipfinder.multicast.TcpDiscoveryMulticastIpFinder;
import org.apache.ignite.spi.discovery.tcp.ipfinder.vm.TcpDiscoveryVmIpFinder;
import org.apache.ignite.spi.metric.jmx.JmxMetricExporterSpi;
import org.apache.ignite.spi.metric.log.LogExporterSpi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.cache.expiry.AccessedExpiryPolicy;
import javax.cache.expiry.Duration;
import java.util.Arrays;

import static org.apache.ignite.cache.CacheWriteSynchronizationMode.FULL_SYNC;
import static org.apache.ignite.cache.CacheWriteSynchronizationMode.PRIMARY_SYNC;

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
        igniteConfiguration.setStripedPoolSize(igniteProperties.getStripedPoolSize());
        igniteConfiguration.setPublicThreadPoolSize(igniteProperties.getPublicThreadPoolSize());
        igniteConfiguration.setSystemThreadPoolSize(igniteProperties.getSystemThreadPoolSize());
        igniteConfiguration.setQueryThreadPoolSize(igniteProperties.getQueryThreadPoolSize());
        igniteConfiguration.setServiceThreadPoolSize(igniteProperties.getServiceThreadPoolSize());

        // Ignite日志
        Logger logger = LoggerFactory.getLogger("org.apache.ignite");
        igniteConfiguration.setGridLogger(new Slf4jLogger(logger));
        igniteConfiguration.setSnapshotPath(igniteProperties.getSnapshotPath());
        igniteConfiguration.setWorkDirectory(igniteProperties.getWorkPath());
        igniteConfiguration.setMetricsLogFrequency(60000);
        igniteConfiguration.setMetricExporterSpi(
                new JmxMetricExporterSpi(),
                new LogExporterSpi()
        );

        TcpCommunicationSpi communicationSpi = new TcpCommunicationSpi();
        communicationSpi.setMessageQueueLimit(igniteProperties.getPublishTps());

        igniteConfiguration.setCommunicationSpi(communicationSpi);

        AtomicConfiguration atomicConfiguration = new AtomicConfiguration()
                .setBackups(igniteProperties.getBackup())
                .setCacheMode(CacheMode.PARTITIONED);

        igniteConfiguration.setAtomicConfiguration(atomicConfiguration);
        // 非持久化数据区域
        DataRegionConfiguration notPersistence = new DataRegionConfiguration().setPersistenceEnabled(false)
                .setInitialSize(igniteProperties.getNotPersistenceInitialSize() * 1024 * 1024)
                .setMaxSize(igniteProperties.getNotPersistenceMaxSize() * 1024 * 1024)
                .setName(NOT_PERSISTENCE_DATA_REGION);
        // 持久化数据区域
        DataRegionConfiguration persistence = new DataRegionConfiguration().setPersistenceEnabled(true)
                .setInitialSize(igniteProperties.getPersistenceInitialSize() * 1024 * 1024)
                .setMaxSize(igniteProperties.getPersistenceMaxSize() * 1024 * 1024)
                .setMetricsEnabled(true)
                .setName(PERSISTENCE_DATA_REGION);
        DataStorageConfiguration dataStorageConfiguration = new DataStorageConfiguration()
                .setDefaultDataRegionConfiguration(notPersistence)
                .setDataRegionConfigurations(persistence)
                .setWalSegmentSize(1024 * 1024 * 1024)
                .setMetricsEnabled(true)
                .setWriteThrottlingEnabled(false)
                .setCdcWalPath(StrUtil.isNotBlank(igniteProperties.getPersistenceStorePath()) ? igniteProperties.getPersistenceStorePath() + "/cdc" : null)
                .setWalArchivePath(StrUtil.isNotBlank(igniteProperties.getPersistenceStorePath()) ? igniteProperties.getPersistenceStorePath() + "/wal/arch" : null)
                .setWalPath(StrUtil.isNotBlank(igniteProperties.getPersistenceStorePath()) ? igniteProperties.getPersistenceStorePath() + "/wal" : null)
                .setStoragePath(StrUtil.isNotBlank(igniteProperties.getPersistenceStorePath()) ? igniteProperties.getPersistenceStorePath() : null);

        dataStorageConfiguration.getDefaultDataRegionConfiguration()
                .setCheckpointPageBufferSize(1024 * 1024 * 1024);

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

        ignite.cluster().baselineAutoAdjustEnabled(true);
        ignite.cluster().baselineAutoAdjustTimeout(20000);


        return ignite;
    }


    @Bean
    public IgniteCache retainMessageCache() throws Exception {
        CacheConfiguration cacheConfiguration = new CacheConfiguration()
                .setExpiryPolicyFactory(AccessedExpiryPolicy.factoryOf(Duration.ONE_DAY))
                .setDataRegionName(PERSISTENCE_DATA_REGION)
                .setBackups(igniteProperties.getBackup())
                .setReadFromBackup(false)
                .setCacheMode(CacheMode.PARTITIONED)
                .setPartitionLossPolicy(PartitionLossPolicy.IGNORE)
                .setAtomicityMode(CacheAtomicityMode.ATOMIC)
                .setStatisticsEnabled(true)
                .setName("retainMessageCache");
        return ignite().getOrCreateCache(cacheConfiguration);
    }

    @Bean
    public IgniteCache subscribeCache() throws Exception {
        CacheConfiguration cacheConfiguration = new CacheConfiguration()
                .setDataRegionName(PERSISTENCE_DATA_REGION)
                .setReadFromBackup(false)
                .setCacheMode(CacheMode.REPLICATED)
                .setWriteSynchronizationMode(FULL_SYNC)
                .setPartitionLossPolicy(PartitionLossPolicy.IGNORE)
                .setAtomicityMode(CacheAtomicityMode.TRANSACTIONAL)
                .setStatisticsEnabled(true)
                .setName("subscribeCache");
        return ignite().getOrCreateCache(cacheConfiguration);
    }

    @Bean
    public IgniteCache subscribeWildCardCache() throws Exception {
        CacheConfiguration cacheConfiguration = new CacheConfiguration()
                .setDataRegionName(PERSISTENCE_DATA_REGION)
                .setReadFromBackup(false)
                .setCacheMode(CacheMode.REPLICATED)
                .setWriteSynchronizationMode(FULL_SYNC)
                .setPartitionLossPolicy(PartitionLossPolicy.IGNORE)
                .setAtomicityMode(CacheAtomicityMode.TRANSACTIONAL)
                .setStatisticsEnabled(true)
                .setName("subscribeWildCardCache");
        return ignite().getOrCreateCache(cacheConfiguration);
    }

    @Bean
    public IgniteCache sessionCache() throws Exception {
        CacheConfiguration cacheConfiguration = new CacheConfiguration()
                .setExpiryPolicyFactory(AccessedExpiryPolicy.factoryOf(Duration.ONE_DAY))
                .setDataRegionName(PERSISTENCE_DATA_REGION)
                .setBackups(igniteProperties.getBackup())
                .setReadFromBackup(true)
                .setCacheMode(CacheMode.PARTITIONED)
                .setPartitionLossPolicy(PartitionLossPolicy.IGNORE)
                .setAtomicityMode(CacheAtomicityMode.ATOMIC)
                .setStatisticsEnabled(true)
                .setOnheapCacheEnabled(true)
                .setCopyOnRead(false)
                .setWriteSynchronizationMode(PRIMARY_SYNC)
                .setName("sessionCache");
        return ignite().getOrCreateCache(cacheConfiguration);
    }

//    @Bean
//    public IgniteCache lockCache() throws Exception {
//        CacheConfiguration cacheConfiguration = new CacheConfiguration()
//                .setExpiryPolicyFactory(AccessedExpiryPolicy.factoryOf(Duration.ONE_DAY))
//                .setDataRegionName(PERSISTENCE_DATA_REGION)
//                .setBackups(igniteProperties.getBackup())
//                .setReadFromBackup(false)
//                .setCacheMode(CacheMode.PARTITIONED)
//                .setPartitionLossPolicy(PartitionLossPolicy.IGNORE)
//                .setAtomicityMode(CacheAtomicityMode.TRANSACTIONAL)
//                .setStatisticsEnabled(true)
//                .setName("lockCache");
//        return ignite().getOrCreateCache(cacheConfiguration);
//    }

    @Bean
    public IgniteCache serverPublishMessageCache() throws Exception {
        CacheConfiguration cacheConfiguration = new CacheConfiguration()
                .setExpiryPolicyFactory(AccessedExpiryPolicy.factoryOf(Duration.ONE_DAY))
                .setDataRegionName(PERSISTENCE_DATA_REGION)
                .setBackups(igniteProperties.getBackup())
                .setReadFromBackup(true)
                .setCacheMode(CacheMode.PARTITIONED)
                .setPartitionLossPolicy(PartitionLossPolicy.IGNORE)
                .setAtomicityMode(CacheAtomicityMode.ATOMIC)
                .setStatisticsEnabled(true)
                .setOnheapCacheEnabled(true)
                .setCopyOnRead(false)
                .setWriteSynchronizationMode(PRIMARY_SYNC)
                .setName("serverPublishMessageCache");
        return ignite().getOrCreateCache(cacheConfiguration);
    }

    @Bean
    public IgniteCache clientPublishMessageCache() throws Exception {
        CacheConfiguration cacheConfiguration = new CacheConfiguration()
                .setExpiryPolicyFactory(AccessedExpiryPolicy.factoryOf(Duration.ONE_DAY))
                .setDataRegionName(PERSISTENCE_DATA_REGION)
                .setBackups(igniteProperties.getBackup())
                .setReadFromBackup(false)
                .setCacheMode(CacheMode.PARTITIONED)
                .setPartitionLossPolicy(PartitionLossPolicy.IGNORE)
                .setAtomicityMode(CacheAtomicityMode.ATOMIC)
                .setStatisticsEnabled(true)
                .setName("clientPublishMessageCache");
        return ignite().getOrCreateCache(cacheConfiguration);
    }

    @Bean
    public IgniteCache transportCache() throws Exception {
        CacheConfiguration cacheConfiguration = new CacheConfiguration()
                .setExpiryPolicyFactory(AccessedExpiryPolicy.factoryOf(Duration.ONE_DAY))
                .setDataRegionName(NOT_PERSISTENCE_DATA_REGION)
                .setReadFromBackup(false)
                .setCacheMode(CacheMode.PARTITIONED)
                .setPartitionLossPolicy(PartitionLossPolicy.IGNORE)
                .setBackups(igniteProperties.getBackup())
                .setStatisticsEnabled(true)
//                .setAtomicityMode(CacheAtomicityMode.TRANSACTIONAL)
                .setName("transportCache");
        return ignite().getOrCreateCache(cacheConfiguration);
    }

    @Bean
    public IgniteCache vertxNodeInfo() throws Exception {
        CacheConfiguration cacheConfiguration = new CacheConfiguration()
                .setDataRegionName(NOT_PERSISTENCE_DATA_REGION)
                .setReadFromBackup(false)
                .setCacheMode(CacheMode.REPLICATED)
                .setAtomicityMode(CacheAtomicityMode.ATOMIC)
                .setWriteSynchronizationMode(FULL_SYNC)
                .setPartitionLossPolicy(PartitionLossPolicy.IGNORE)
                .setName("__vertx.nodeInfo");
        log.info("Creating Vertx node info cache with configuration: {}", cacheConfiguration);
        return ignite().getOrCreateCache(cacheConfiguration);
    }

    @Bean
    public IgniteCache vertxSubs() throws Exception {
        CacheConfiguration cacheConfiguration = new CacheConfiguration()
                .setDataRegionName(NOT_PERSISTENCE_DATA_REGION)
                .setReadFromBackup(false)
                .setCacheMode(CacheMode.REPLICATED)
                .setAtomicityMode(CacheAtomicityMode.ATOMIC)
                .setWriteSynchronizationMode(FULL_SYNC)
                .setPartitionLossPolicy(PartitionLossPolicy.IGNORE)
                .setName("__vertx.subs");
        log.info("Creating Vertx subscriptions cache with configuration: {}", cacheConfiguration);
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
