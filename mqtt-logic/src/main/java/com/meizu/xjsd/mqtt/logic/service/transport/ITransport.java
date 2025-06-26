package com.meizu.xjsd.mqtt.logic.service.transport;

import com.meizu.xjsd.mqtt.logic.entity.IMqttAuth;
import com.meizu.xjsd.mqtt.logic.entity.IMqttWill;
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
}
