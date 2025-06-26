package com.meizu.xjsd.mqtt.store.memory;

import com.meizu.xjsd.mqtt.logic.primitive.TopicUtils;
import com.meizu.xjsd.mqtt.logic.service.store.IRetainMessageStoreService;
import com.meizu.xjsd.mqtt.logic.service.store.RetainMessageStoreDTO;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class MemRetainMessageStoreService implements IRetainMessageStoreService {

    private ConcurrentHashMap<String, RetainMessageStoreDTO> store = new ConcurrentHashMap<>();

    @Override
    public void put(String topic, RetainMessageStoreDTO retainMessageStoreDTO) {
        store.put(topic, retainMessageStoreDTO);
    }

    @Override
    public RetainMessageStoreDTO get(String topic) {
        return store.get(topic);
    }

    @Override
    public void remove(String topic) {
        store.remove(topic);
    }

    @Override
    public boolean containsKey(String topic) {
        return store.containsKey(topic);
    }

    @Override
    public List<RetainMessageStoreDTO> search(String topicFilter) {
        return store.keySet().stream()
                .filter(topic -> TopicUtils.isMatchNew(topic, topicFilter))
                .map(store::get)
                .collect(Collectors.toList());
    }
}
