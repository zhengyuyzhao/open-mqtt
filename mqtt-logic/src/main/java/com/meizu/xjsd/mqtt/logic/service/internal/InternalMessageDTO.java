/**
 * Copyright (c) 2018, Mr.Wang (recallcode@aliyun.com) All rights reserved.
 */

package com.meizu.xjsd.mqtt.logic.service.internal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 内部消息
 */
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class InternalMessageDTO implements Serializable {

    private static final long serialVersionUID = -1L;

    //当前频道clientId
    private String clientId;

    private String topic;

    private int mqttQoS;

    private byte[] messageBytes;

    private boolean retain;

    private boolean dup;


}
