package com.meizu.xjsd.mqtt.broker.adapter;

import com.meizu.xjsd.mqtt.logic.entity.IMqttAuth;
import com.meizu.xjsd.mqtt.logic.entity.IMqttWill;
import com.meizu.xjsd.mqtt.logic.service.transport.ITransport;
import io.netty.handler.codec.mqtt.MqttConnectReturnCode;
import io.netty.handler.codec.mqtt.MqttProperties;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.vertx.core.buffer.Buffer;
import io.vertx.mqtt.MqttEndpoint;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class VertxTransportAdp implements ITransport {
    private MqttEndpoint  mqttEndpoint;

    public static ITransport of(MqttEndpoint mqttEndpoint) {
        if (mqttEndpoint == null) {
            return null;
        }
        VertxTransportAdp adp = new VertxTransportAdp();
        adp.mqttEndpoint = mqttEndpoint;
        return adp;
    }

    @Override
    public String clientIdentifier() {
        return mqttEndpoint.clientIdentifier();
    }

    @Override
    public IMqttAuth auth() {
        return VertxAuthAdp.of(mqttEndpoint.auth());
    }

    @Override
    public void reject(MqttConnectReturnCode returnCode) {
        mqttEndpoint.reject(returnCode);
    }

    @Override
    public boolean isConnected() {
        return mqttEndpoint.isConnected();
    }

    @Override
    public IMqttWill will() {
        return VertxMqttWillAdp.of(mqttEndpoint.will());
    }

    @Override
    public int protocolVersion() {
        return mqttEndpoint.protocolVersion();
    }

    @Override
    public String protocolName() {
        return mqttEndpoint.protocolName();
    }

    @Override
    public boolean isCleanSession() {
        return mqttEndpoint.isCleanSession();
    }

    @Override
    public int keepAliveTimeSeconds() {
        return mqttEndpoint.keepAliveTimeSeconds();
    }

    @Override
    public int lastMessageId() {
        return mqttEndpoint.lastMessageId();
    }

    @Override
    public void subscribeAcknowledge(int subscribeMessageId, List<MqttQoS> grantedQoSLevels) {
        mqttEndpoint.subscribeAcknowledge(subscribeMessageId, grantedQoSLevels);
    }

    @Override
    public void close() {
        mqttEndpoint.close();
    }

    @Override
    public Integer publish(String topic, byte[] payload, MqttQoS qosLevel, boolean isDup, boolean isRetain) {
        return mqttEndpoint.publish(topic, Buffer.buffer( payload), qosLevel, isDup, isRetain).await();
    }

    @Override
    public Integer publish(String topic, byte[] payload, MqttQoS qosLevel, boolean isDup, boolean isRetain, int messageId) {
        return mqttEndpoint.publish(topic, Buffer.buffer(payload), qosLevel, isDup, isRetain, messageId).await();
    }

    @Override
    public Integer publish(String topic, byte[] payload, MqttQoS qosLevel, boolean isDup, boolean isRetain, int messageId, MqttProperties properties) {
        return mqttEndpoint.publish(topic, Buffer.buffer(payload), qosLevel, isDup, isRetain, messageId, properties).await();
    }
}
