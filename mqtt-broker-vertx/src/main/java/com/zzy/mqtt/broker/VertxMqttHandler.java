package com.zzy.mqtt.broker;

import com.zzy.mqtt.broker.adapter.*;
import com.zzy.mqtt.logic.MqttLogic;
import io.vertx.core.Future;
import io.vertx.core.VerticleBase;
import io.vertx.mqtt.MqttServer;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class VertxMqttHandler extends VerticleBase {
    private MqttServer mqttServer;
    private MqttLogic mqttLogic;

    public VertxMqttHandler(MqttLogic mqttLogic) {
        this.mqttLogic = mqttLogic;

    }


    @Override
    public Future<?> start() throws Exception {
        this.mqttServer = MqttServer.create(vertx);
        mqttServer.endpointHandler(endpoint -> {
            endpoint.subscriptionAutoAck(false);
            endpoint.publishAutoAck(false);
            mqttLogic.connect().handle(VertxTransportAdp.of(endpoint));
            endpoint.publishHandler(message -> {
                mqttLogic.publish().handle(VertxMqttPublishMessageAdp.of(message), VertxTransportAdp.of(endpoint));
            }).subscribeHandler(subscribe -> {
                mqttLogic.subscribe().handle(VertxMqttSubscribeMessageAdp.of(subscribe), VertxTransportAdp.of(endpoint));
            }).unsubscribeHandler(unsubscribe -> {
                mqttLogic.unSubscribe().handle(VertxMqttUnsubscribeMessageAdp.of(unsubscribe), VertxTransportAdp.of(endpoint));
            }).publishCompletionMessageHandler(mess -> {
                mqttLogic.publishComp().handle(VertxMqttPubCompMessageAdp.of(mess), VertxTransportAdp.of(endpoint));
            }).publishAcknowledgeMessageHandler(ackMessage -> {
                mqttLogic.publishAck().handle(VertxMqttPubAckMessageAdp.of(ackMessage), VertxTransportAdp.of(endpoint));
            }).disconnectMessageHandler(close -> {
                mqttLogic.disConnect().handle(VertxTransportAdp.of(endpoint));
            }).publishReleaseHandler(id -> {
                mqttLogic.pubrel().handle(id, VertxTransportAdp.of(endpoint));
            }).publishReceivedHandler(id -> {
                mqttLogic.pubrec().handle(id, VertxTransportAdp.of(endpoint));
            }).exceptionHandler(throwable -> {
                log.error("MQTT endpoint exception: {}", throwable.getMessage());
            });


//            endpoint.accept(mqttLogic.isSessionPresent(endpoint.clientIdentifier()));

        });
        return mqttServer.listen(mqttLogic.getMqttLogicConfig().getServerPort());
    }

    @Override
    public Future<?> stop() throws Exception {
        return mqttServer.close();
    }
}
