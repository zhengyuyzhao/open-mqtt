package com.meizu.xjsd.mqtt.logic.service.internal;

public interface IInternalMessageService {
    void internalPublish(InternalMessageDTO internalMessageDTO) throws Exception;

    void internalSubscribe(InternalMessageDTO internalMessageDTO);

//    void registerInternalMessageListener(String topic);
//
//    void unregisterInternalMessageListener(String topic);
}
