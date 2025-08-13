/**
 * Copyright (c) 2018, Mr.Wang (recallcode@aliyun.com) All rights reserved.
 */

package com.zzy.mqtt.logic.service.store;

import lombok.*;

import java.io.Serializable;

/**
 * PUBLISH重发消息存储
 */
@Builder
@Data
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ClientPublishMessageStoreDTO implements Serializable {


	private String clientId;

	private String topic;

	private int mqttQoS;

	private int messageId;

	private byte[] messageBytes;

	private boolean isHandshakeOk; // 是否握手成功

	private long createTime = System.currentTimeMillis(); // 创建时间


}
