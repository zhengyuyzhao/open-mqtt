package com.zzy.mqtt.broker;

import com.zzy.mqtt.broker.cluster.VertxCluster;
import com.zzy.mqtt.logic.MqttBroker;
import com.zzy.mqtt.logic.MqttLogic;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;

public class VertxMqttBroker implements MqttBroker {

    private VertxMqttHandler mqttServer;
    private String id;
    private VertxCluster vertxCluster;

    public VertxMqttBroker(MqttLogic mqttLogic, VertxCluster vertxCluster) {
        this.mqttServer = new VertxMqttHandler(mqttLogic);
        this.vertxCluster = vertxCluster;
    }

    @Override
    public void start() {
        id = Vertx.vertx().deployVerticle(mqttServer, new DeploymentOptions()
//                .setThreadingModel(ThreadingModel.VIRTUAL_THREAD)
                )
                .await();
    }

    @Override
    public void stop() {
        Vertx.vertx().undeploy(id).await();
    }
}
