package com.meizu.xjsd.mqtt.store.memory;

import com.meizu.xjsd.mqtt.logic.service.store.IMessageIdService;
import lombok.RequiredArgsConstructor;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.IgniteTransactions;
import org.apache.ignite.transactions.Transaction;

@RequiredArgsConstructor
public class IgniteMessageIdService implements IMessageIdService {
    private final Ignite ignite;
    private final IgniteCache<String, Long> cache;

    @Override
    public int getNextMessageId(String clientId) {
        IgniteTransactions transactions = ignite.transactions();
        Long count = 0l;
        try (Transaction tx = transactions.txStart()) {
            count = cache.get(clientId);
            if (count == null) {
                count = 0L;
            }
            cache.put(clientId, count + 1);
            tx.commit();
        }
        return count.intValue() % 65535;
    }
}
