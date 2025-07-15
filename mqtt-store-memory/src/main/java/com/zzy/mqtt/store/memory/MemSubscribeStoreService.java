package com.zzy.mqtt.store.memory;

import com.zzy.mqtt.logic.primitive.TopicUtils;
import com.zzy.mqtt.logic.service.store.ISubscribeStoreService;
import com.zzy.mqtt.logic.service.store.SubscribeStoreDTO;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class MemSubscribeStoreService implements ISubscribeStoreService {

    private ConcurrentHashMap<String, Map<String, SubscribeStoreDTO>> subscribeStore = new ConcurrentHashMap<>();

    @Override
    public void put(String topicFilter, SubscribeStoreDTO subscribeStoreDTO) {
        subscribeStore.computeIfAbsent(topicFilter, k -> new HashMap<>()).put(subscribeStoreDTO.getClientId(), subscribeStoreDTO);
    }

    @Override
    public void remove(String topicFilter, String clientId) {
        subscribeStore.computeIfAbsent(topicFilter, k -> new HashMap<>()).remove(clientId);
    }

    @Override
    public SubscribeStoreDTO get(String topicFilter, String clientId) {
        return null;
    }

    @Override
    public void removeForClient(String clientId) {
        subscribeStore.values().forEach(
                map -> map.remove(clientId)
        );
    }

    @Override
    public List<SubscribeStoreDTO> search(String topic) {
        return subscribeStore.entrySet().stream()
                .filter(entry -> TopicUtils.isMatchNew(entry.getKey(), topic))
                .flatMap(entry -> entry.getValue().values().stream())
                .collect(Collectors.toList());
    }
}
