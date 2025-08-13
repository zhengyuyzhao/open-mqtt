/**
 * Copyright (c) 2018, Mr.Wang (recallcode@aliyun.com) All rights reserved.
 */

package com.zzy.mqtt.logic.service.store;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Retain标志消息存储
 */
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RetainMessageStoreDTO implements Serializable {


	private String topic;

	private byte[] messageBytes;

	private int mqttQoS;

	public String getTopic() {
		return topic;
	}

	public RetainMessageStoreDTO setTopic(String topic) {
		this.topic = topic;
		return this;
	}

	public byte[] getMessageBytes() {
		return messageBytes;
	}

	public RetainMessageStoreDTO setMessageBytes(byte[] messageBytes) {
		this.messageBytes = messageBytes;
		return this;
	}

	public int getMqttQoS() {
		return mqttQoS;
	}

	public RetainMessageStoreDTO setMqttQoS(int mqttQoS) {
		this.mqttQoS = mqttQoS;
		return this;
	}
}
