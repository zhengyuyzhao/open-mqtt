/**
 * Copyright (c) 2018, Mr.Wang (recallcode@aliyun.com) All rights reserved.
 */

package com.zzy.mqtt.logic.service.store;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * PUBLISH重发消息存储
 */
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ServerPublishMessageStoreDTO implements Serializable {


	private String fromClientId;

	private String clientId;

	private String topic;

	private int mqttQoS;

	private int messageId;

	private byte[] messageBytes;

	private long createTime;

	private int times = 0; // 重发次数

}
