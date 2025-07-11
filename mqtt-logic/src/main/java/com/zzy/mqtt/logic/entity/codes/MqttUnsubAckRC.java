package com.zzy.mqtt.logic.entity.codes;

public enum MqttUnsubAckRC implements IMqttReasonCode{

    SUCCESS((byte)0x0),
    NO_SUBSCRIPTION_EXISTED((byte)0x11),
    UNSPECIFIED_ERROR((byte)0x80),
    IMPLEMENTATION_SPECIFIC_ERROR((byte)0x83),
    NOT_AUTHORIZED((byte)0x87),
    TOPIC_FILTER_INVALID((byte)0x8F),
    PACKET_IDENTIFIER_IN_USE((byte)0x91);

    MqttUnsubAckRC(byte byteValue) {
        this.byteValue = byteValue;
    }

    public static MqttUnsubAckRC fromByte(byte byteValue) {
        for (MqttUnsubAckRC rc : MqttUnsubAckRC.values()) {
            if (rc.byteValue == byteValue) {
                return rc;
            }
        }
        return UNSPECIFIED_ERROR; // Default to UNSPECIFIED_ERROR if not found
    }

    private final byte byteValue;

    public byte value() {
        return byteValue;
    }


}
