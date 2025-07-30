package com.zzy.mqtt.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.binder.jvm.ExecutorServiceMetrics;
import lombok.RequiredArgsConstructor;

import java.util.Collections;
import java.util.concurrent.ExecutorService;

@RequiredArgsConstructor
public class ExecutorsMetrics {

    private final MeterRegistry meterRegistry;

    public void registerExecutor(String executorName, ExecutorService executorService) {
        new ExecutorServiceMetrics(executorService, executorName, Collections.singleton(Tag.of("executor.name", executorName)))
                .bindTo(meterRegistry);
    }
}
