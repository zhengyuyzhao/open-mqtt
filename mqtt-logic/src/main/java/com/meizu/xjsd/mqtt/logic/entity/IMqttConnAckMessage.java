/*
 * Copyright 2016 Red Hat Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.meizu.xjsd.mqtt.logic.entity;

import com.meizu.xjsd.mqtt.logic.entity.codes.IMqttReturnCode;
import io.netty.handler.codec.mqtt.MqttProperties;

/**
 * Represents an MQTT CONNACK message
 */

public interface IMqttConnAckMessage {


    IMqttReturnCode code();

    /**
     * @return is an old session is present
     */

    boolean isSessionPresent();


    MqttProperties properties();
}
