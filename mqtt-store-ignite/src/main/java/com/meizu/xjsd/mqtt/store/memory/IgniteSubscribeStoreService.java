package com.meizu.xjsd.mqtt.store.memory;

import com.meizu.xjsd.mqtt.logic.primitive.TopicUtils;
import com.meizu.xjsd.mqtt.logic.service.store.ISubscribeStoreService;
import com.meizu.xjsd.mqtt.logic.service.store.SubscribeStoreDTO;
import lombok.RequiredArgsConstructor;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.IgniteTransactions;
import org.apache.ignite.transactions.Transaction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
public class IgniteSubscribeStoreService implements ISubscribeStoreService {
    private final Ignite ignite;
    private final IgniteCache<String, Map<String, SubscribeStoreDTO>> subscribeStore;

//    private ConcurrentHashMap<String, Map<String, SubscribeStoreDTO>> subscribeStore = new ConcurrentHashMap<>();

    @Override
    public void put(String topicFilter, SubscribeStoreDTO subscribeStoreDTO) {
        IgniteTransactions transactions = ignite.transactions();
        try (Transaction tx = transactions.txStart()) {
            Map<String, SubscribeStoreDTO> subscriptions = subscribeStore.get(topicFilter);
            if (subscriptions == null) {
                subscriptions = new HashMap<>();
            }
            subscriptions.put(subscribeStoreDTO.getClientId(), subscribeStoreDTO);
            subscribeStore.put(topicFilter, subscriptions);
            tx.commit();
        }
    }

    @Override
    public void remove(String topicFilter, String clientId) {
        IgniteTransactions transactions = ignite.transactions();
        try (Transaction tx = transactions.txStart()) {
            Map<String, SubscribeStoreDTO> subscriptions = subscribeStore.get(topicFilter);
            if (subscriptions != null) {
                subscriptions.remove(clientId);
                subscribeStore.put(topicFilter, subscriptions);
            }

            tx.commit();
        }
    }

    @Override
    public void removeForClient(String clientId) {
        subscribeStore.forEach((stringMapEntry -> {
            Map<String, SubscribeStoreDTO> subscriptions = stringMapEntry.getValue();
            if (subscriptions != null) {
                subscriptions.remove(clientId);
                subscribeStore.put(stringMapEntry.getKey(), subscriptions);
            }
        }));
    }

    @Override
    public List<SubscribeStoreDTO> search(String topic) {
        List<SubscribeStoreDTO> result = new ArrayList<>();
        subscribeStore.forEach(stringMapEntry -> {
            if (TopicUtils.isMatchNew(stringMapEntry.getKey(), topic)) {
                result.addAll(stringMapEntry.getValue().values());
            }
        });
        return result;
    }
}
