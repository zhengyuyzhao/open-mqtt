package com.meizu.xjsd.mqtt.logic.service.handler;

import com.meizu.xjsd.mqtt.logic.service.transport.ITransport;

@FunctionalInterface
public interface MessageHandler<E> {
    void handle(E event, ITransport transport);
}
