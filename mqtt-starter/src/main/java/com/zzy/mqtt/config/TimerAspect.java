package com.zzy.mqtt.config;

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
        ThrowableHolder holder = new ThrowableHolder();
        Object result = timer.recordCallable(() -> {
            try {
                return joinPoint.proceed();
            } catch (Throwable e) {
                holder.throwable = e;
            }
            return null;
        });
        if (null != holder.throwable) {
            throw holder.throwable;
        }
        return result;
    }



    private class ThrowableHolder {

        Throwable throwable;
    }
}
