package com.zzy.mqtt.logic.service.handler;

import com.zzy.mqtt.logic.service.transport.ITransport;

@FunctionalInterface
public interface MessageHandler<E> {
    void handle(E event, ITransport transport);
}
