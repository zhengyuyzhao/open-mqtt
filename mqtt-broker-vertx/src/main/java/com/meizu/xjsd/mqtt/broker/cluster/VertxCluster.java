package com.meizu.xjsd.mqtt.broker.cluster;

import io.vertx.core.Vertx;
import io.vertx.core.spi.cluster.ClusterManager;

public class VertxCluster {

    private Vertx vertx;

    public Vertx getVertx() {
        return vertx;
    }

    public VertxCluster(ClusterManager clusterManager) {
        if (clusterManager == null) {
            vertx = Vertx.vertx();
        }
        vertx = Vertx.builder().withClusterManager(clusterManager)
                .buildClustered().await();
    }
}
