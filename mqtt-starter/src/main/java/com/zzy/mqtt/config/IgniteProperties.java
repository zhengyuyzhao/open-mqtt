/**
 * Copyright (c) 2018, Mr.Wang (recallcode@aliyun.com) All rights reserved.
 */

package com.zzy.mqtt.config;

import lombok.Data;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * ignite属性配置
 */
@Data
@ToString
@ConfigurationProperties(prefix = "spring.mqtt.store.ignite")
public class IgniteProperties {
	private boolean enableMulticastGroup = false;

	private String multicastGroup;

	private String[] staticIpAddresses;

	/**
	 * 持久化缓存内存初始化大小(MB), 默认值: 64
	 */
	private int persistenceInitialSize = 64;

	/**
	 * 持久化缓存占用内存最大值(MB), 默认值: 128
	 */
	private int persistenceMaxSize = 128;

	/**
	 * 持久化磁盘存储路径
	 */
	private String persistenceStorePath = "/data/ignite/store";

	private String snapshotPath = "/data/ignite/store/snapshot";

	private String workPath = "/data/ignite/workPath";

	/**
	 * 非持久化缓存内存初始化大小(MB), 默认值: 64
	 */
	private int notPersistenceInitialSize = 64;

	/**
	 * 非持久化缓存占用内存最大值(MB), 默认值: 128
	 */
	private int notPersistenceMaxSize = 128;

	private int systemThreadPoolSize = 16;

	private int queryThreadPoolSize = 16;

	private int stripedPoolSize = 16;

	private int publicThreadPoolSize = 16;

	private int serviceThreadPoolSize = 16;

	private int publishTps = 5000;

	private int backup = 1;


}
