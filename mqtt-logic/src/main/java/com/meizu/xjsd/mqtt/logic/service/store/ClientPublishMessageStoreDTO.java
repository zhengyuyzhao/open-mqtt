/**
 * Copyright (c) 2018, Mr.Wang (recallcode@aliyun.com) All rights reserved.
 */

package com.meizu.xjsd.mqtt.logic.service.store;

import lombok.Builder;
import lombok.Data;
import lombok.ToString;

import java.io.Serializable;

/**
 * PUBLISH重发消息存储
 */
@Builder
@Data
@ToString
public class ClientPublishMessageStoreDTO implements Serializable {

	private static final long serialVersionUID = -8112511377194421600L;

	private String clientId;

	private String topic;

	private int mqttQoS;

	private int messageId;

	private byte[] messageBytes;

	private boolean isHandshakeOk; // 是否握手成功

	private long createTime = System.currentTimeMillis(); // 创建时间


}
