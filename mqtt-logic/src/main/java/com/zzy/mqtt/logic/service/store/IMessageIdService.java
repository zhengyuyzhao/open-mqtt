/**
 * Copyright (c) 2018, Mr.Wang (recallcode@aliyun.com) All rights reserved.
 */

package com.zzy.mqtt.logic.service.store;

/**
 * 分布式生成报文标识符
 */
public interface IMessageIdService {

	/**
	 * 获取报文标识符
	 */
	int getNextMessageId(String clientId);
}
