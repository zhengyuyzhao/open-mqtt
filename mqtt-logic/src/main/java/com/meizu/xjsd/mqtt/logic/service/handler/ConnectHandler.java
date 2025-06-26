package com.meizu.xjsd.mqtt.logic.service.handler;

@FunctionalInterface
public interface ConnectHandler<ITransport> {
    void handle(ITransport transport);
}
