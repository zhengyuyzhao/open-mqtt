package com.zzy.mqtt.logic.entity;

import com.zzy.mqtt.logic.entity.codes.IMqttReasonCode;
import io.netty.handler.codec.mqtt.MqttProperties;

/**
 * Represents an MQTT AUTH message
 */

public interface IMqttAuthenticationExchangeMessage {


    /**
     * @return authenticate reason code
     */
    IMqttReasonCode reasonCode();

    /**
     * @return authenticate method
     */

    String authenticationMethod();

    /**
     * @return authentication data
     */

    String authenticationData();

    /**
     * @return MQTT properties
     */

    MqttProperties properties();
}
