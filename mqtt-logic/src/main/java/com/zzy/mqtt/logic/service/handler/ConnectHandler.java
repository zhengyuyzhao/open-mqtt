package com.zzy.mqtt.logic.service.handler;

@FunctionalInterface
public interface ConnectHandler<ITransport> {
    void handle(ITransport transport);
}
