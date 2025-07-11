package com.zzy.mqtt.store.memory;

import com.zzy.mqtt.logic.service.store.IServerPublishMessageStoreService;
import com.zzy.mqtt.logic.service.store.ServerPublishMessageStoreDTO;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.IgniteTransactions;
import org.apache.ignite.transactions.Transaction;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Builder
@RequiredArgsConstructor
public class IgniteServerPublishMessageStoreService implements IServerPublishMessageStoreService<ServerPublishMessageStoreDTO> {
    private final Ignite ignite;
    private final IgniteCache<String, Map<Integer, ServerPublishMessageStoreDTO>> store;

    @Override
    public void put(String clientId, ServerPublishMessageStoreDTO dupPublishMessageStoreDTO) {
        IgniteTransactions transactions = ignite.transactions();
        try (Transaction tx = transactions.txStart()) {
            Map<Integer, ServerPublishMessageStoreDTO> map = store.get(clientId);
            if (map == null) {
                map = new HashMap<>();
            }
            map.put(dupPublishMessageStoreDTO.getMessageId(), dupPublishMessageStoreDTO);
            store.put(clientId, map);

            tx.commit();
        }
    }


    @Override
    public List<ServerPublishMessageStoreDTO> get(String clientId) {
        return Optional.ofNullable(store.get(clientId)).orElse(new HashMap<>()).values().stream().toList();
    }

    @Override
    public ServerPublishMessageStoreDTO get(String clientId, int messageId) {
        return Optional.ofNullable(store.get(clientId)).orElse(new HashMap<>()).get(messageId);
    }

    @Override
    public void remove(String clientId, int messageId) {
        IgniteTransactions transactions = ignite.transactions();
        try (Transaction tx = transactions.txStart()) {
            Map<Integer, ServerPublishMessageStoreDTO> map = store.get(clientId);
            if (map != null) {
                map.remove(messageId);
                store.put(clientId, map);
            }

            tx.commit();
        }

    }

    @Override
    public void removeByClient(String clientId) {
        store.remove(clientId);
    }
}
