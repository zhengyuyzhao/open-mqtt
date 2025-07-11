package com.zzy.mqtt.store.memory;

import com.zzy.mqtt.logic.service.store.IServerPublishMessageStoreService;
import com.zzy.mqtt.logic.service.store.ServerPublishMessageStoreDTO;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class MemDupPublishMessageStoreService implements IServerPublishMessageStoreService<ServerPublishMessageStoreDTO> {
    private ConcurrentHashMap<String, List<ServerPublishMessageStoreDTO>> store = new ConcurrentHashMap<>();

    @Override
    public void put(String clientId, ServerPublishMessageStoreDTO dupPublishMessageStoreDTO) {
        store.computeIfAbsent(clientId, k -> new java.util.ArrayList<>()).add(dupPublishMessageStoreDTO);
    }


    @Override
    public List<ServerPublishMessageStoreDTO> get(String clientId) {
        return store.get(clientId);
    }

    @Override
    public ServerPublishMessageStoreDTO get(String clientId, int messageId) {
        return null;
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
