package com.meizu.xjsd.mqtt.logic.service.handler;

@FunctionalInterface
public interface DisConnectHandler<ITransport> {
    void handle(ITransport transport);
}
