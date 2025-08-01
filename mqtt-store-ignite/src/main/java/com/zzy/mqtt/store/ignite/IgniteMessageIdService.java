package com.zzy.mqtt.store.ignite;

import com.zzy.mqtt.logic.service.store.IMessageIdService;
import lombok.RequiredArgsConstructor;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteAtomicSequence;

@RequiredArgsConstructor
public class IgniteMessageIdService implements IMessageIdService {
    private final Ignite ignite;

    @Override
    public int getNextMessageId(String clientId) {
        IgniteAtomicSequence count = ignite.atomicSequence(clientId, 1, true);
        return (int) ((count.incrementAndGet() % 65530 + 65530) % 65530) + 1;
    }


}
