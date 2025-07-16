package com.zzy.mqtt.store.ignite;

import cn.hutool.core.collection.CollectionUtil;
import com.zzy.mqtt.logic.primitive.TopicUtils;
import com.zzy.mqtt.logic.service.store.ISubscribeStoreService;
import com.zzy.mqtt.logic.service.store.SubscribeStoreDTO;
import lombok.RequiredArgsConstructor;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.IgniteTransactions;
import org.apache.ignite.transactions.Transaction;
import org.apache.ignite.transactions.TransactionConcurrency;
import org.apache.ignite.transactions.TransactionIsolation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
public class IgniteSubscribeStoreService implements ISubscribeStoreService {
    private final Ignite ignite;
    private final IgniteCache<String, Map<String, SubscribeStoreDTO>> subscribeWildCardStore;
    private final IgniteCache<String, Map<String, SubscribeStoreDTO>> subscribeStore;

//    private ConcurrentHashMap<String, Map<String, SubscribeStoreDTO>> subscribeStore = new ConcurrentHashMap<>();

    @Override
    public void put(String topicFilter, SubscribeStoreDTO subscribeStoreDTO) {
        IgniteTransactions transactions = ignite.transactions();
        try (Transaction tx = transactions.txStart(TransactionConcurrency.OPTIMISTIC,
                TransactionIsolation.REPEATABLE_READ)) {
            if (TopicUtils.isWildCard(topicFilter)) {
                Map<String, SubscribeStoreDTO> subscriptions = subscribeWildCardStore.get(topicFilter);
                if (subscriptions == null) {
                    subscriptions = new HashMap<>();
                }
                subscriptions.put(subscribeStoreDTO.getClientId(), subscribeStoreDTO);
                subscribeWildCardStore.put(topicFilter, subscriptions);
            } else {
                Map<String, SubscribeStoreDTO> subscriptions = subscribeStore.get(topicFilter);
                if (subscriptions == null) {
                    subscriptions = new HashMap<>();
                }
                subscriptions.put(subscribeStoreDTO.getClientId(), subscribeStoreDTO);
                subscribeStore.put(topicFilter, subscriptions);
            }
            tx.commit();
        }
    }

    @Override
    public void remove(String topicFilter, String clientId) {
        IgniteTransactions transactions = ignite.transactions();
        try (Transaction tx = transactions.txStart(TransactionConcurrency.OPTIMISTIC,
                TransactionIsolation.REPEATABLE_READ)) {
            if (TopicUtils.isWildCard(topicFilter)) {
                Map<String, SubscribeStoreDTO> subscriptions = subscribeWildCardStore.get(topicFilter);
                if (subscriptions != null) {
                    subscriptions.remove(clientId);
                    subscribeWildCardStore.put(topicFilter, subscriptions);
                }
            } else {
                Map<String, SubscribeStoreDTO> subscriptions = subscribeStore.get(topicFilter);
                if (subscriptions != null) {
                    subscriptions.remove(clientId);
                    subscribeWildCardStore.put(topicFilter, subscriptions);
                }
            }

            tx.commit();
        }
    }

    @Override
    public SubscribeStoreDTO get(String topicFilter, String clientId) {
        if (TopicUtils.isWildCard(topicFilter)) {
            Map<String, SubscribeStoreDTO> subscriptions = subscribeWildCardStore.get(topicFilter);
            if (subscriptions != null) {
                return subscriptions.get(clientId);
            }
        } else {
            Map<String, SubscribeStoreDTO> subscriptions = subscribeStore.get(topicFilter);
            if (subscriptions != null) {
                return subscriptions.get(clientId);
            }
        }
        return null;

    }

    @Override
    public void removeForClient(String clientId) {
        subscribeWildCardStore.forEach((stringMapEntry -> {
            Map<String, SubscribeStoreDTO> subscriptions = stringMapEntry.getValue();
            if (subscriptions != null) {
                subscriptions.remove(clientId);
                subscribeWildCardStore.put(stringMapEntry.getKey(), subscriptions);
            }
        }));

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

        subscribeWildCardStore.forEach(stringMapEntry -> {
            if (TopicUtils.isMatchNew(stringMapEntry.getKey(), topic)) {
                Map<String, SubscribeStoreDTO> map = stringMapEntry.getValue();
                if (CollectionUtil.isNotEmpty(map)) {
                    result.addAll(map.values());
                }
            }
        });

        Map<String, SubscribeStoreDTO> map = subscribeStore.get(topic);
        if (CollectionUtil.isNotEmpty(map)) {
            result.addAll(map.values());
        }
        return result;
    }
}
