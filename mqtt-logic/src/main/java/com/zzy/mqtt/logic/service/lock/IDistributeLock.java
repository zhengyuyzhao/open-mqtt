package com.zzy.mqtt.logic.service.lock;

import java.util.concurrent.locks.Lock;

public interface IDistributeLock {
    Lock getLock(String key);
}
