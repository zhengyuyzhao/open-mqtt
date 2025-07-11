package com.zzy.mqtt.logic.entity.codes;


import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.handler.codec.mqtt.MqttVersion;

/**
 * Reason codes for SUBACK MQTT message
 */
public enum MqttSubAckRC implements IMqttReasonCode {
    //All MQTT versions
    GRANTED_QOS0((byte) 0x0),
    GRANTED_QOS1((byte) 0x1),
    GRANTED_QOS2((byte) 0x2),
    UNSPECIFIED_ERROR((byte) 0x80),
    //MQTT5 or higher
    IMPLEMENTATION_SPECIFIC_ERROR((byte) 0x83),
    NOT_AUTHORIZED((byte) 0x87),
    TOPIC_FILTER_INVALID((byte) 0x8F),
    PACKET_IDENTIFIER_IN_USE((byte) 0x91),
    QUOTA_EXCEEDED((byte) 0x97),
    SHARED_SUBSCRIPTIONS_NOT_SUPPORTED((byte) 0x9E),
    SUBSCRIPTION_IDENTIFIERS_NOT_SUPPORTED((byte) 0xA1),
    WILDCARD_SUBSCRIPTIONS_NOT_SUPPORTED((byte) 0xA2);

    MqttSubAckRC(byte byteValue) {
        this.byteValue = byteValue;
    }



    private final byte byteValue;

    public byte value() {
        return byteValue;
    }

    public static MqttSubAckRC fromByte(byte byteValue) {
        for (MqttSubAckRC rc : MqttSubAckRC.values()) {
            if (rc.byteValue == byteValue) {
                return rc;
            }
        }
        return UNSPECIFIED_ERROR; // Default to UNSPECIFIED_ERROR if not found
    }

    public static MqttSubAckRC qosGranted(MqttQoS qos) {
        switch (qos) {
            case AT_MOST_ONCE:
                return MqttSubAckRC.GRANTED_QOS0;
            case AT_LEAST_ONCE:
                return MqttSubAckRC.GRANTED_QOS1;
            case EXACTLY_ONCE:
                return MqttSubAckRC.GRANTED_QOS2;
            case FAILURE:
                return MqttSubAckRC.UNSPECIFIED_ERROR;
            default:
                return MqttSubAckRC.UNSPECIFIED_ERROR;
        }
    }

    public MqttSubAckRC limitForMqttVersion(MqttVersion version) {
        if (version != MqttVersion.MQTT_5 && byteValue > UNSPECIFIED_ERROR.byteValue)
            return UNSPECIFIED_ERROR;
        else
            return this;
    }
}
