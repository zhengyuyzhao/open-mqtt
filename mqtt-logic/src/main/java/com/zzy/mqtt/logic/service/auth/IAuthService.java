/**
 * Copyright (c) 2018, Mr.Wang (recallcode@aliyun.com) All rights reserved.
 */

package com.zzy.mqtt.logic.service.auth;

import com.zzy.mqtt.logic.entity.IMqttAuth;

/**
 * 用户和密码认证服务接口
 */
public interface IAuthService {

	/**
	 * 验证用户名和密码是否正确
	 */
	boolean checkValid(IMqttAuth mqttAuth);

}
