package com.zzy.mqtt.store.memory;

import com.zzy.mqtt.logic.service.store.IMessageIdService;

import java.util.concurrent.atomic.AtomicInteger;

public class MemMessageIdService implements IMessageIdService {
    private AtomicInteger messageIdCounter = new AtomicInteger(0);
    @Override
    public int getNextMessageId(String clientId) {
        return messageIdCounter.incrementAndGet();
    }
}
