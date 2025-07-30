package com.zzy.mqtt.store.ignite;

import com.zzy.mqtt.logic.primitive.TopicUtils;
import com.zzy.mqtt.logic.service.store.ISubscribeStoreService;
import com.zzy.mqtt.logic.service.store.SubscribeStoreDTO;
import lombok.RequiredArgsConstructor;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
public class IgniteSubscribeStoreService implements ISubscribeStoreService {
    private final Ignite ignite;
    private final IgniteCache<String, SubscribeStoreDTO> subscribeWildCardStore;
    private final IgniteCache<String, SubscribeStoreDTO> subscribeStore;
    private final String INFIX = "&&";

//    private ConcurrentHashMap<String, Map<String, SubscribeStoreDTO>> subscribeStore = new ConcurrentHashMap<>();

    @Override
    public void put(String topicFilter, SubscribeStoreDTO subscribeStoreDTO) {

        if (TopicUtils.isWildCard(topicFilter)) {
            subscribeWildCardStore.put(topicFilter + INFIX + subscribeStoreDTO.getClientId(),
                    subscribeStoreDTO);
        } else {
            subscribeStore.put(topicFilter + INFIX + subscribeStoreDTO.getClientId(),
                    subscribeStoreDTO);
        }

    }

    @Override
    public void remove(String topicFilter, String clientId) {

        if (TopicUtils.isWildCard(topicFilter)) {
            subscribeWildCardStore.remove(topicFilter + INFIX + clientId);
        } else {
            subscribeStore.remove(topicFilter + INFIX + clientId);
        }

    }

    @Override
    public SubscribeStoreDTO get(String topicFilter, String clientId) {
        return subscribeStore.get(topicFilter + INFIX + clientId);

    }

    @Override
    public void removeForClient(String clientId) {
        subscribeWildCardStore.forEach((stringMapEntry -> {
            if (stringMapEntry.getValue().getClientId().equals(clientId)) {
                subscribeWildCardStore.remove(stringMapEntry.getKey());
            }
        }));

        subscribeStore.forEach((stringMapEntry -> {
            if (stringMapEntry.getValue().getClientId().equals(clientId)) {
                subscribeStore.remove(stringMapEntry.getKey());
            }
        }));
    }

    @Override
    public List<SubscribeStoreDTO> search(String topic) {
        List<SubscribeStoreDTO> result = new ArrayList<>();

        subscribeWildCardStore.forEach(stringMapEntry -> {
            String key = stringMapEntry.getKey().split(INFIX)[0]; // 获取topicFilter
            if (TopicUtils.isMatchNew(key, topic)) {
                result.add(stringMapEntry.getValue());
            }
        });

        subscribeStore.forEach(stringMapEntry -> {
            String key = stringMapEntry.getKey().split(INFIX)[0]; // 获取topicFilter
            if (key.equals(topic)) {
                result.add(stringMapEntry.getValue());
            }
        });

        return result;
    }
}
