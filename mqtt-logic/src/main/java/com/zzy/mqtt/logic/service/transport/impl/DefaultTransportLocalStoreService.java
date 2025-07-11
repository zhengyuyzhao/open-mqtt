package com.zzy.mqtt.logic.service.transport.impl;

import com.zzy.mqtt.logic.service.transport.ITransport;
import com.zzy.mqtt.logic.service.transport.ITransportLocalStoreService;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

public class DefaultTransportLocalStoreService implements ITransportLocalStoreService {
    private ConcurrentHashMap<String, ITransport> transportStore = new ConcurrentHashMap<>();

    @Override
    public Collection<ITransport> list() {
        return transportStore.values();
    }

    @Override
    public ITransport getTransport(String clientId) {
        return transportStore.get(clientId);
    }

    @Override
    public void putTransport(String clientId, ITransport transport) {
        transportStore.put(clientId, transport);
    }

    @Override
    public void removeTransport(String clientId) {
        transportStore.remove(clientId);
    }
}
