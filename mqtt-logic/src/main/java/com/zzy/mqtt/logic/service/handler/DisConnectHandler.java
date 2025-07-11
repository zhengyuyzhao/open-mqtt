package com.zzy.mqtt.logic.service.handler;

@FunctionalInterface
public interface DisConnectHandler<ITransport> {
    void handle(ITransport transport);
}
