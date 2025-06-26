/**
 * Copyright (c) 2018, Mr.Wang (recallcode@aliyun.com) All rights reserved.
 */

package com.meizu.xjsd.mqtt.logic.service.store;

import java.io.Serializable;

/**
 * 订阅存储
 */
public class SubscribeStoreDTO implements Serializable {

	private static final long serialVersionUID = 1276156087085594264L;

	private String clientId;

	private String topicFilter;

	private int mqttQoS;

	public SubscribeStoreDTO(String clientId, String topicFilter, int mqttQoS) {
		this.clientId = clientId;
		this.topicFilter = topicFilter;
		this.mqttQoS = mqttQoS;
	}

	public String getClientId() {
		return clientId;
	}

	public SubscribeStoreDTO setClientId(String clientId) {
		this.clientId = clientId;
		return this;
	}

	public String getTopicFilter() {
		return topicFilter;
	}

	public SubscribeStoreDTO setTopicFilter(String topicFilter) {
		this.topicFilter = topicFilter;
		return this;
	}

	public int getMqttQoS() {
		return mqttQoS;
	}

	public SubscribeStoreDTO setMqttQoS(int mqttQoS) {
		this.mqttQoS = mqttQoS;
		return this;
	}
}
