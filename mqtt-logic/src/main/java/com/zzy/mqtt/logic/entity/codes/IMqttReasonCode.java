package com.zzy.mqtt.logic.entity.codes;

/**
 * Common interface for MQTT messages reason codes enums
 */
public interface IMqttReasonCode {
    byte value();

    default boolean isError() {
        return (value() & 0x80) != 0;
    }

}
