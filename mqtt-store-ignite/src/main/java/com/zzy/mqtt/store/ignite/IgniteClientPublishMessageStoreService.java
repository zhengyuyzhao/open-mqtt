package com.zzy.mqtt.store.ignite;

import com.zzy.mqtt.logic.service.store.ClientPublishMessageStoreDTO;
import com.zzy.mqtt.logic.service.store.IClientPublishMessageStoreService;
import lombok.RequiredArgsConstructor;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.cache.query.QueryCursor;
import org.apache.ignite.cache.query.ScanQuery;
import org.apache.ignite.lang.IgniteBiPredicate;

import javax.cache.Cache;
import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
public class IgniteClientPublishMessageStoreService implements IClientPublishMessageStoreService<ClientPublishMessageStoreDTO> {
    private final IgniteCache<String, ClientPublishMessageStoreDTO> store;
    private String INFIX = "-";

    @Override
    public void put(String clientId, ClientPublishMessageStoreDTO dupPublishMessageStoreDTO) {
        store.put(clientId + INFIX + dupPublishMessageStoreDTO.getMessageId(),
                dupPublishMessageStoreDTO);
    }


//    @Override
//    public List<ClientPublishMessageStoreDTO> get(String clientId) {
//        return Optional.ofNullable(store.get(clientId)).orElse(new HashMap<>()).values().stream().toList();
//    }

    @Override
    public ClientPublishMessageStoreDTO get(String clientId, int messageId) {
        return store.get(clientId + INFIX + messageId);
    }

    @Override
    public List<ClientPublishMessageStoreDTO> get(String clientId) {
        IgniteBiPredicate<String, ClientPublishMessageStoreDTO> filter = (key, p) -> p.getClientId() == clientId;
        List<ClientPublishMessageStoreDTO> result = new ArrayList<>();
        try (QueryCursor<Cache.Entry<String, ClientPublishMessageStoreDTO>> qryCursor
                     = store.query(new ScanQuery<>(filter))) {
            qryCursor.forEach(
                    entry -> result.add(entry.getValue()));
        }
        return result;
    }

    @Override
    public List<ClientPublishMessageStoreDTO> getAll() {
        List<ClientPublishMessageStoreDTO> result = new ArrayList<>();
        try (QueryCursor<Cache.Entry<String, ClientPublishMessageStoreDTO>> qryCursor
                     = store.query(new ScanQuery<>())) {
            qryCursor.forEach(
                    entry -> result.add(entry.getValue()));
        }
        return result;
    }

    @Override
    public void remove(String clientId, int messageId) {
        store.remove(clientId + INFIX + messageId);
    }

    @Override
    public void removeByClient(String clientId) {
        IgniteBiPredicate<String, ClientPublishMessageStoreDTO> filter = (key, p) -> p.getClientId() == clientId;
        try (QueryCursor<Cache.Entry<String, ClientPublishMessageStoreDTO>> qryCursor
                     = store.query(new ScanQuery<>(filter))) {
            qryCursor.forEach(
                    entry -> store.remove(entry.getKey()));
        }

    }
}
