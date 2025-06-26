package com.meizu.xjsd.mqtt.logic.service.transport;

import java.util.Collection;

public interface ITransportLocalStoreService {

    Collection<ITransport> list();

    ITransport getTransport(String clientId);

    void putTransport(String clientId, ITransport transport);

    void removeTransport(String clientId);
}
