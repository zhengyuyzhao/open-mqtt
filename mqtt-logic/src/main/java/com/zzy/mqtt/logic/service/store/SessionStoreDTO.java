/**
 * Copyright (c) 2018, Mr.Wang (recallcode@aliyun.com) All rights reserved.
 */

package com.zzy.mqtt.logic.service.store;


import cn.hutool.core.bean.BeanUtil;
import com.zzy.mqtt.logic.entity.MqttWill;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Map;

/**
 * 会话存储
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionStoreDTO implements Serializable {

    private String clientId;

    private int expire;

    private boolean cleanSession;

    private MqttWill willMessage;

    private Map<String, Object> willMessageMap;

    public MqttWill getWillMessage() {
        if (willMessage == null && willMessageMap != null) {
            return BeanUtil.toBean(willMessageMap, MqttWill.class);
        }
        return willMessage;
    }

    public void setWillMessage(MqttWill willMessage) {
        if (willMessage != null) {
            this.willMessageMap = BeanUtil.beanToMap(willMessage);
        }
        this.willMessage = willMessage;
    }
}
