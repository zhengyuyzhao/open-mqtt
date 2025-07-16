package com.zzy.mqtt.store.ignite;

import com.zzy.mqtt.logic.service.lock.IDistributeLock;
import lombok.RequiredArgsConstructor;
import org.apache.ignite.IgniteCache;

import java.util.concurrent.locks.Lock;

@RequiredArgsConstructor
public class IgniteLockService implements IDistributeLock {
    private final IgniteCache<String, Integer> store;


    @Override
    public Lock getLock(String key) {
        return store.lock(key);
    }
}
