package com.zzy.mqtt.store.ignite;

import com.zzy.mqtt.logic.service.store.IMessageIdService;
import lombok.RequiredArgsConstructor;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteAtomicSequence;

import java.util.WeakHashMap;

@RequiredArgsConstructor
public class IgniteMessageIdService implements IMessageIdService {
    private final Ignite ignite;
    private WeakHashMap<String, IgniteAtomicSequence> clientIdSeqMap = new WeakHashMap<>();


    @Override
    public int getNextMessageId(String clientId) {
        IgniteAtomicSequence count = clientIdSeqMap.get(clientId);
        if (count == null) {
            count = ignite.atomicSequence(clientId, 1, true);
            clientIdSeqMap.put(clientId, count);
        }
        return (int) ((count.incrementAndGet() % 65530 + 65530) % 65530) + 1;
    }


}
