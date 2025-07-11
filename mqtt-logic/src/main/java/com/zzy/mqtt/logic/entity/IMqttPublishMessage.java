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

package com.zzy.mqtt.logic.entity;

import io.netty.handler.codec.mqtt.MqttProperties;
import io.netty.handler.codec.mqtt.MqttQoS;

/**
 * Represents an MQTT PUBLISH message
 */

public interface IMqttPublishMessage extends IMqttMessage {


    /**
     * @return Quality of service level
     */

    MqttQoS qosLevel();

    /**
     * @return If the message is a duplicate
     */
    boolean isDup();

    /**
     * @return If the message needs to be retained
     */
    boolean isRetain();

    /**
     * @return Topic on which the message was published
     */
    String topicName();

    /**
     * @return Payload message
     */
    byte[] payload();


    /**
     * @return MQTT properties
     */
    MqttProperties properties();
}
