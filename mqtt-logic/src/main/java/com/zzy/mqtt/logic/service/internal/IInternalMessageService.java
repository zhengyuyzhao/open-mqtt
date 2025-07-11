package com.zzy.mqtt.logic.service.internal;

public interface IInternalMessageService {
    void internalPublish(InternalMessageDTO internalMessageDTO);

    void internalSubscribe(InternalMessageDTO internalMessageDTO);

//    void registerInternalMessageListener(String topic);
//
//    void unregisterInternalMessageListener(String topic);
}
