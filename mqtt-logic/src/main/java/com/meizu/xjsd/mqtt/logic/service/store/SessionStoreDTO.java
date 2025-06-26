/**
 * Copyright (c) 2018, Mr.Wang (recallcode@aliyun.com) All rights reserved.
 */

package com.meizu.xjsd.mqtt.logic.service.store;


import com.meizu.xjsd.mqtt.logic.entity.MqttWill;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

/**
 * 会话存储
 */
@Data
@Builder
public class SessionStoreDTO implements Serializable {
    private static final long serialVersionUID = -1L;

    private String clientId;

    private int expire;

    private boolean cleanSession;

    private MqttWill willMessage;


}
