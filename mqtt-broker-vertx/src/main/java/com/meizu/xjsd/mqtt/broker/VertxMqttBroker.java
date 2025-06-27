package com.meizu.xjsd.mqtt.broker;

import com.meizu.xjsd.mqtt.broker.cluster.VertxCluster;
import com.meizu.xjsd.mqtt.logic.MqttBroker;
import com.meizu.xjsd.mqtt.logic.MqttLogic;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.ThreadingModel;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;

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
