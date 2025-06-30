package com.meizu.xjsd.mqtt.logic.service.transport;

public interface IClientStoreService {

    String getBroker(String clientId);

    void putClient(String clientId, String broker);

    void removeClient(String clientId);
}
