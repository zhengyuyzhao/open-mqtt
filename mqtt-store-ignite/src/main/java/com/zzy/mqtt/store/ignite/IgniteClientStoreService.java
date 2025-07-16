package com.zzy.mqtt.store.ignite;

import com.zzy.mqtt.logic.service.transport.IClientStoreService;
import lombok.RequiredArgsConstructor;
import org.apache.ignite.IgniteCache;

@RequiredArgsConstructor
public class IgniteClientStoreService implements IClientStoreService {
    private final IgniteCache<String, String> store;

    @Override
    public String getBroker(String clientId) {
        return store.get(clientId);
    }

    @Override
    public void putClient(String clientId, String broker) {
        store.put(clientId, broker);
    }

    @Override
    public void removeClient(String clientId) {
        store.remove(clientId);
    }
}
