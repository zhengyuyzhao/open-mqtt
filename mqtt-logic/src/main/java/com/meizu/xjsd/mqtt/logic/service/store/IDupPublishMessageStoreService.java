/**
 * Copyright (c) 2018, Mr.Wang (recallcode@aliyun.com) All rights reserved.
 */

package com.meizu.xjsd.mqtt.logic.service.store;

import java.util.List;

/**
 * PUBLISH重发消息存储服务接口, 当QoS=1和QoS=2时存在该重发机制
 */
public interface IDupPublishMessageStoreService {

	/**
	 * 存储消息
	 */
	void put(String clientId, DupPublishMessageStoreDTO dupPublishMessageStoreDTO);

	void put(String clientId, DupPublishMessageStoreDTO dupPublishMessageStoreDTO, int expire);

	/**
	 * 获取消息集合
	 */
	List<DupPublishMessageStoreDTO> get(String clientId);

	/**
	 * 删除消息
	 */
	void remove(String clientId, int messageId);

	/**
	 * 删除消息
	 */
	void removeByClient(String clientId);

}
