package com.meizu.xjsd.mqtt.logic.service.handler.impl;

import com.meizu.xjsd.mqtt.logic.MqttLogic;
import com.meizu.xjsd.mqtt.logic.entity.MqttWill;
import com.meizu.xjsd.mqtt.logic.service.handler.ConnectHandler;
import com.meizu.xjsd.mqtt.logic.service.store.DupPublishMessageStoreDTO;
import com.meizu.xjsd.mqtt.logic.service.store.IDupPublishMessageStoreService;
import com.meizu.xjsd.mqtt.logic.service.store.ISessionStoreService;
import com.meizu.xjsd.mqtt.logic.service.store.SessionStoreDTO;
import com.meizu.xjsd.mqtt.logic.service.transport.ITransport;
import com.meizu.xjsd.mqtt.logic.service.transport.ITransportLocalStoreService;
import io.netty.handler.codec.mqtt.MqttQoS;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class MqttAuthHandler implements ConnectHandler<ITransport> {
;


    @Override
    public void handle(ITransport transport) {
        MqttLogic.getExecutorService().execute(() -> this.handleInner(transport));
    }

    private void handleInner(ITransport transport) {


    }




}
