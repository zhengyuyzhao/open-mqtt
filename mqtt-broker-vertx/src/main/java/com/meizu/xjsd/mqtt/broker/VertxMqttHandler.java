package com.meizu.xjsd.mqtt.broker;

import com.meizu.xjsd.mqtt.broker.adapter.*;
import com.meizu.xjsd.mqtt.broker.cluster.VertxCluster;
import com.meizu.xjsd.mqtt.logic.MqttLogic;
import io.vertx.core.*;
import io.vertx.mqtt.MqttServer;

public class VertxMqttHandler extends VerticleBase {
    private MqttServer mqttServer;
    private MqttLogic mqttLogic;

    public VertxMqttHandler(MqttLogic mqttLogic) {
        Vertx vertx = Vertx.vertx();
        this.mqttServer = MqttServer.create(vertx);
        this.mqttLogic = mqttLogic;
        mqttServer.endpointHandler(endpoint -> {
            endpoint.subscriptionAutoAck(true);
            endpoint.publishAutoAck(true);
            mqttLogic.connect().handle(VertxTransportAdp.of(endpoint));
            endpoint.publishHandler(message -> {
                mqttLogic.publish().handle(VertxMqttPublishMessageAdp.of(message), VertxTransportAdp.of(endpoint));
            }).subscribeHandler(subscribe -> {
                mqttLogic.subscribe().handle(VertxMqttSubscribeMessageAdp.of(subscribe),VertxTransportAdp.of(endpoint));
            }).unsubscribeHandler(unsubscribe -> {
                mqttLogic.unSubscribe().handle(VertxMqttUnsubscribeMessageAdp.of(unsubscribe), VertxTransportAdp.of(endpoint));
            }).publishCompletionMessageHandler(mess -> {
                mqttLogic.publishComp().handle(VertxMqttPubCompMessageAdp.of(mess), VertxTransportAdp.of(endpoint));
            }).publishAcknowledgeMessageHandler(ackMessage -> {
                mqttLogic.publishAck().handle(VertxMqttPubAckMessageAdp.of(ackMessage), VertxTransportAdp.of(endpoint));
            }).disconnectMessageHandler(close -> {
                mqttLogic.disConnect().handle(VertxTransportAdp.of(endpoint));
            });


            endpoint.accept(mqttLogic.isSessionPresent(endpoint.clientIdentifier()));

        });
    }


    @Override
    public Future<?> start() throws Exception {
        return mqttServer.listen(mqttLogic.getMqttLogicConfig().getServerPort());
    }

    @Override
    public Future<?> stop() throws Exception {
        return mqttServer.close();
    }
}
