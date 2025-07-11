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

package com.zzy.mqtt.logic.entity.prop;

import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.handler.codec.mqtt.MqttSubscriptionOption;

/**
 * Represents a subscription to a topic
 */

public interface IMqttTopicSubscription {

    /**
     * @return Subscription topic name
     */

    String topicName();

    /**
     * @return Quality of Service level for the subscription
     */

    MqttQoS qualityOfService();

    /**
     * Subscription option
     *
     * @return Subscription options
     */

    MqttSubscriptionOption subscriptionOption();

}
