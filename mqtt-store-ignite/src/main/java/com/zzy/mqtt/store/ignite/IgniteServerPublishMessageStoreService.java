package com.zzy.mqtt.store.ignite;

import com.zzy.mqtt.logic.service.store.IServerPublishMessageStoreService;
import com.zzy.mqtt.logic.service.store.ServerPublishMessageStoreDTO;
import lombok.Builder;
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
public class IgniteServerPublishMessageStoreService implements IServerPublishMessageStoreService<ServerPublishMessageStoreDTO> {
    private final IgniteCache<String, ServerPublishMessageStoreDTO> store;
    private String INFIX = "-";

    @Override
    public void put(String clientId, ServerPublishMessageStoreDTO dupPublishMessageStoreDTO) {
        store.put(clientId + INFIX + dupPublishMessageStoreDTO.getMessageId(),
                dupPublishMessageStoreDTO);
    }


    @Override
    public List<ServerPublishMessageStoreDTO> get(String clientId) {
        IgniteBiPredicate<String, ServerPublishMessageStoreDTO> filter = (key, p) -> clientId.equals(p.getClientId());
        List<ServerPublishMessageStoreDTO> result = new ArrayList<>();
        ScanQuery<String, ServerPublishMessageStoreDTO> scanQuery = new ScanQuery<>();
        scanQuery.setFilter(filter);
        scanQuery.setPageSize(10);
        try (QueryCursor<Cache.Entry<String, ServerPublishMessageStoreDTO>> qryCursor
                     = store.query(new ScanQuery<>(filter))) {
            qryCursor.forEach(
                    entry -> result.add(entry.getValue()));
        }
        return result;
    }

    @Override
    public ServerPublishMessageStoreDTO get(String clientId, int messageId) {
        return store.get(clientId + INFIX + messageId);
    }

    @Override
    public void remove(String clientId, int messageId) {
        store.remove(clientId + INFIX + messageId);

    }

    @Override
    public void removeByClient(String clientId) {
        IgniteBiPredicate<String, ServerPublishMessageStoreDTO> filter = (key, p) -> clientId.equals(p.getClientId());
        try (QueryCursor<Cache.Entry<String, ServerPublishMessageStoreDTO>> qryCursor
                     = store.query(new ScanQuery<>(filter))) {
            qryCursor.forEach(
                    entry -> store.remove(entry.getKey()));
        }
    }
}
