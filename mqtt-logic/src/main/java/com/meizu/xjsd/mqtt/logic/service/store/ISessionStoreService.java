/**
 * Copyright (c) 2018, Mr.Wang (recallcode@aliyun.com) All rights reserved.
 */

package com.meizu.xjsd.mqtt.logic.service.store;

/**
 * 会话存储服务接口
 */
public interface ISessionStoreService {

    /**
     * 存储会话
     */
    void put(String clientId, SessionStoreDTO sessionStoreDTO, int expire);

    void put(String clientId, SessionStoreDTO sessionStoreDTO);

    /**
     * 设置session失效时间
     */
    void expire(String clientId, int expire);

    /**
     * 获取会话
     */
    SessionStoreDTO get(String clientId);

    /**
     * clientId的会话是否存在
     */
    boolean containsKey(String clientId);

    /**
     * 删除会话
     */
    void remove(String clientId);

}
