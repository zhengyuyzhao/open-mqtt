/**
 * Copyright (c) 2018, Mr.Wang (recallcode@aliyun.com) All rights reserved.
 */

package com.meizu.xjsd.mqtt.logic.service.store;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

/**
 * PUBLISH重发消息存储
 */
@Builder
@Data
public class ServerPublishMessageStoreDTO implements Serializable {

	private static final long serialVersionUID = -8112511377194421600L;

	private String clientId;

	private String topic;

	private int mqttQoS;

	private int messageId;

	private byte[] messageBytes;

	private long createTime;

}
