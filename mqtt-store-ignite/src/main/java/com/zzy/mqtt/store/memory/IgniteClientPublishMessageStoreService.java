package com.zzy.mqtt.store.memory;

import com.zzy.mqtt.logic.service.store.ClientPublishMessageStoreDTO;
import com.zzy.mqtt.logic.service.store.IClientPublishMessageStoreService;
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
public class IgniteClientPublishMessageStoreService implements IClientPublishMessageStoreService<ClientPublishMessageStoreDTO> {
    private final Ignite ignite;
    private final IgniteCache<String, Map<Integer, ClientPublishMessageStoreDTO>> store;

    @Override
    public void put(String clientId, ClientPublishMessageStoreDTO dupPublishMessageStoreDTO) {
        IgniteTransactions transactions = ignite.transactions();
        try (Transaction tx = transactions.txStart()) {
            Map<Integer, ClientPublishMessageStoreDTO> map = store.get(clientId);
            if (map == null) {
                map = new HashMap<>();
            }
            map.put(dupPublishMessageStoreDTO.getMessageId(), dupPublishMessageStoreDTO);
            store.put(clientId, map);

            tx.commit();
        }
    }


//    @Override
//    public List<ClientPublishMessageStoreDTO> get(String clientId) {
//        return Optional.ofNullable(store.get(clientId)).orElse(new HashMap<>()).values().stream().toList();
//    }

    @Override
    public ClientPublishMessageStoreDTO get(String clientId, int messageId) {
        return Optional.ofNullable(store.get(clientId)).orElse(new HashMap<>()).get(messageId);
    }

    @Override
    public List<ClientPublishMessageStoreDTO> get(String clientId) {
        return Optional.ofNullable(store.get(clientId)).orElse(new HashMap<>()).values().stream().toList();
    }

    @Override
    public void remove(String clientId, int messageId) {
        IgniteTransactions transactions = ignite.transactions();
        try (Transaction tx = transactions.txStart()) {
            Map<Integer, ClientPublishMessageStoreDTO> map = store.get(clientId);
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
