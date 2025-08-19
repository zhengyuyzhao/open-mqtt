package com.zzy.mqtt.config;

import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Timer;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;


@Component
@Aspect
public class TimerAspect {


    @Around(value = "execution(* com.zzy.mqtt.store.ignite..*.*(..))")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {

        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        Timer timer = Metrics.timer("ignite.cost.time", "method.name", className + "-" + methodName);
        DistributionSummary igniteCostTimeSummary = DistributionSummary.builder("ignite.cost.time.summary")
                .description("Ignite cost time summary")
                .tag("method.name", className + "-" + methodName)
                .publishPercentiles(0.5, 0.75, 0.95, 0.99)
                .register(Metrics.globalRegistry);

        ThrowableHolder holder = new ThrowableHolder();
        long startTime = System.currentTimeMillis();
        Object result = timer.recordCallable(() -> {
            try {
                return joinPoint.proceed();
            } catch (Throwable e) {
                holder.throwable = e;
            }
            return null;
        });
        igniteCostTimeSummary.record(System.currentTimeMillis() - startTime);
        if (null != holder.throwable) {
            throw holder.throwable;
        }
        return result;
    }


    private static class ThrowableHolder {

        Throwable throwable;
    }
}
