package com.zzy.mqtt.store.memory;

import com.zzy.mqtt.logic.service.store.IMessageIdService;
import lombok.RequiredArgsConstructor;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteAtomicLong;
import org.apache.ignite.IgniteCache;

@RequiredArgsConstructor
public class IgniteMessageIdService implements IMessageIdService {
    private final Ignite ignite;

    @Override
    public int getNextMessageId(String clientId) {
        IgniteAtomicLong count = ignite.atomicLong(clientId, 1, true);
        return (int) (count.incrementAndGet() % 65535);
    }


}
