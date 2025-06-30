package com.meizu.xjsd.mqtt.logic.service.transport;

import com.meizu.xjsd.mqtt.logic.entity.IMqttAuth;
import com.meizu.xjsd.mqtt.logic.entity.IMqttWill;
import com.meizu.xjsd.mqtt.logic.entity.codes.IMqttReasonCode;
import com.meizu.xjsd.mqtt.logic.entity.codes.MqttSubAckRC;
import com.meizu.xjsd.mqtt.logic.entity.codes.MqttUnsubAckRC;
import io.netty.handler.codec.mqtt.MqttConnectReturnCode;
import io.netty.handler.codec.mqtt.MqttProperties;
import io.netty.handler.codec.mqtt.MqttQoS;

import java.util.List;

public interface ITransport {

    String clientIdentifier();

    IMqttAuth auth();

    void reject(MqttConnectReturnCode returnCode);

    boolean isConnected();


    IMqttWill will();


    int protocolVersion();


    String protocolName();


    boolean isCleanSession();


    int keepAliveTimeSeconds();


    int lastMessageId();

    void subscribeAcknowledge(int subscribeMessageId, List<MqttQoS> grantedQoSLevels);

    void close();


    Integer publish(String topic, byte[] payload, MqttQoS qosLevel, boolean isDup, boolean isRetain);


    Integer publish(String topic, byte[] payload, MqttQoS qosLevel, boolean isDup, boolean isRetain, int messageId);

    Integer publish(String topic, byte[] payload, MqttQoS qosLevel, boolean isDup, boolean isRetain, int messageId, MqttProperties properties);

    void publishAcknowledge(int publishMessageId, IMqttReasonCode reasonCode, MqttProperties properties);

    void publishReceived(int publishMessageId, IMqttReasonCode reasonCode, MqttProperties properties);

    void publishAcknowledge(int publishMessageId);

    void publishReceived(int publishMessageId);

    void publishRelease(int publishMessageId);

    void subscribeAcknowledge(int subscribeMessageId, List<MqttSubAckRC> reasonCodes, MqttProperties properties);

    void unsubscribeAcknowledge(int unsubscribeMessageId);

    void unsubscribeAcknowledge(int unsubscribeMessageId, List<MqttUnsubAckRC> reasonCodes, MqttProperties properties);

    void publishComplete(int publishMessageId);

    void accept(boolean sessionPresent);

}
