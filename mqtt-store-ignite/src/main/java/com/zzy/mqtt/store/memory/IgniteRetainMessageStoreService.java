package com.zzy.mqtt.store.memory;

import cn.hutool.core.util.StrUtil;
import com.zzy.mqtt.logic.primitive.TopicUtils;
import com.zzy.mqtt.logic.service.store.IRetainMessageStoreService;
import com.zzy.mqtt.logic.service.store.RetainMessageStoreDTO;
import lombok.RequiredArgsConstructor;
import org.apache.ignite.IgniteCache;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
public class IgniteRetainMessageStoreService implements IRetainMessageStoreService {

    private final IgniteCache<String, RetainMessageStoreDTO> store;


    @Override
    public void put(String topic, RetainMessageStoreDTO retainMessageStoreDTO) {
        store.put(topic, retainMessageStoreDTO);
    }

    @Override
    public RetainMessageStoreDTO get(String topic) {
        return store.get(topic);
    }

    @Override
    public void remove(String topic) {
        store.remove(topic);
    }

    @Override
    public boolean containsKey(String topic) {
        return store.containsKey(topic);
    }

    @Override
    public List<RetainMessageStoreDTO> search(String topicFilter) {

        List<RetainMessageStoreDTO> retainMessageStores = new ArrayList<>();
        if (!StrUtil.contains(topicFilter, '#') && !StrUtil.contains(topicFilter, '+')) {
            if (store.containsKey(topicFilter)) {
                retainMessageStores.add(store.get(topicFilter));
            }
        } else {
            store.forEach(entry -> {
                String topic = entry.getKey();
                if (TopicUtils.isMatchNew(topic, topicFilter)) {
                    retainMessageStores.add(entry.getValue());
                }
            });
        }
        return retainMessageStores;
    }
}
