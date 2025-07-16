package com.zzy.mqtt.store.ignite;

import com.zzy.mqtt.logic.service.store.ISessionStoreService;
import com.zzy.mqtt.logic.service.store.SessionStoreDTO;
import lombok.RequiredArgsConstructor;
import org.apache.ignite.IgniteCache;

@RequiredArgsConstructor
public class IgniteSessionStoreService implements ISessionStoreService {
    private final IgniteCache<String, SessionStoreDTO> store;
//    private ConcurrentHashMap<String, SessionStoreDTO> store = new ConcurrentHashMap<>();

    @Override
    public void put(String clientId, SessionStoreDTO sessionStoreDTO, int expire) {
        store.put(clientId, sessionStoreDTO);
    }

    @Override
    public void put(String clientId, SessionStoreDTO sessionStoreDTO) {
        store.put(clientId, sessionStoreDTO);
    }

    @Override
    public void expire(String clientId, int expire) {

    }

    @Override
    public SessionStoreDTO get(String clientId) {
        return store.get(clientId);
    }

    @Override
    public boolean containsKey(String clientId) {
        return store.containsKey(clientId);
    }

    @Override
    public void remove(String clientId) {
        store.remove(clientId);
    }
}
