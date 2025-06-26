package com.meizu.xjsd.mqtt.store.memory;

import com.meizu.xjsd.mqtt.logic.service.store.DupPublishMessageStoreDTO;
import com.meizu.xjsd.mqtt.logic.service.store.IDupPublishMessageStoreService;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class MemDupPublishMessageStoreService implements IDupPublishMessageStoreService {
    private ConcurrentHashMap<String, List<DupPublishMessageStoreDTO>> store = new ConcurrentHashMap<>();

    @Override
    public void put(String clientId, DupPublishMessageStoreDTO dupPublishMessageStoreDTO) {
        store.computeIfAbsent(clientId, k -> new java.util.ArrayList<>()).add(dupPublishMessageStoreDTO);
    }

    @Override
    public void put(String clientId, DupPublishMessageStoreDTO dupPublishMessageStoreDTO, int expire) {
        store.computeIfAbsent(clientId, k -> new java.util.ArrayList<>()).add(dupPublishMessageStoreDTO);
    }

    @Override
    public List<DupPublishMessageStoreDTO> get(String clientId) {
        return store.get(clientId);
    }

    @Override
    public void remove(String clientId, int messageId) {
        store.computeIfPresent(clientId, (k, v) -> {
            v.removeIf(dto -> dto.getMessageId() == messageId);
            return v.isEmpty() ? null : v; // Remove the key if the list is empty
        });
    }

    @Override
    public void removeByClient(String clientId) {
        store.remove(clientId);
    }
}
