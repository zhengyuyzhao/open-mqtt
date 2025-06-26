package com.meizu.xjsd.mqtt.store.memory;

import com.meizu.xjsd.mqtt.logic.service.store.DupPublishMessageStoreDTO;
import com.meizu.xjsd.mqtt.logic.service.store.IDupPublishMessageStoreService;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.IgniteTransactions;
import org.apache.ignite.cache.CacheAtomicityMode;
import org.apache.ignite.cache.CacheMode;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.transactions.Transaction;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Builder
@RequiredArgsConstructor
public class IgniteDupPublishMessageStoreService implements IDupPublishMessageStoreService {
    private final Ignite ignite;
    private final IgniteCache<String, List<DupPublishMessageStoreDTO>> store;


    @Override
    public void put(String clientId, DupPublishMessageStoreDTO dupPublishMessageStoreDTO) {
        IgniteTransactions transactions = ignite.transactions();
        try (Transaction tx = transactions.txStart()) {
            List<DupPublishMessageStoreDTO> list = store.get(clientId);
            if (list == null) {
                list = new java.util.ArrayList<>();
                list.add(dupPublishMessageStoreDTO);
                store.put(clientId, list);
            }

            tx.commit();
        }
    }

    @Override
    public void put(String clientId, DupPublishMessageStoreDTO dupPublishMessageStoreDTO, int expire) {
        put(clientId, dupPublishMessageStoreDTO);
    }

    @Override
    public List<DupPublishMessageStoreDTO> get(String clientId) {
        return store.get(clientId);
    }

    @Override
    public void remove(String clientId, int messageId) {
        IgniteTransactions transactions = ignite.transactions();
        try (Transaction tx = transactions.txStart()) {
            List<DupPublishMessageStoreDTO> list = store.get(clientId);
            if (list != null) {
                list.removeIf(dto -> dto.getMessageId() == messageId);
                store.put(clientId, list);
            }

            tx.commit();
        }

    }

    @Override
    public void removeByClient(String clientId) {
        store.remove(clientId);
    }
}
