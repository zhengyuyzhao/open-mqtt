package com.zzy.mqtt.broker.cluster;

import io.micrometer.core.instrument.MeterRegistry;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.spi.cluster.ClusterManager;
import io.vertx.micrometer.MicrometerMetricsFactory;
import io.vertx.micrometer.MicrometerMetricsOptions;
import io.vertx.micrometer.VertxPrometheusOptions;

public class VertxCluster {

    private Vertx vertx;

    public Vertx getVertx() {
        return vertx;
    }

    public VertxCluster(ClusterManager clusterManager, MeterRegistry meterRegistry) {
        VertxOptions vertxOptions = new VertxOptions()
                .setMetricsOptions(
                        new MicrometerMetricsOptions()
                                .setPrometheusOptions(
                                        new VertxPrometheusOptions()
                                                .setEnabled(true)
                                )
                                .setRegistryName("PrometheusMeterRegistry")
                                .setEnabled(true)
                )
                .setEventLoopPoolSize(10) // Adjust based on your needs
                .setWorkerPoolSize(20) // Adjust based on your needs
                .setMaxEventLoopExecuteTime(2000000L); // Set max event loop execute time in nanoseconds

        if (clusterManager == null) {
            vertx = Vertx.vertx(vertxOptions);
        }
        vertx = Vertx.builder()
                .withMetrics(new MicrometerMetricsFactory(meterRegistry))
                .withClusterManager(clusterManager)
                .with(vertxOptions)
                .buildClustered().await();
    }
}
