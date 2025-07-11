package com.zzy.mqtt.logic.service.handler.impl;

import com.zzy.mqtt.logic.MqttLogic;
import com.zzy.mqtt.logic.service.handler.ConnectHandler;
import com.zzy.mqtt.logic.service.transport.ITransport;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

@RequiredArgsConstructor
public class MqttAuthHandler implements ConnectHandler<ITransport> {

    @SneakyThrows
    @Override
    public void handle(ITransport transport) {

        MqttLogic.getExecutorService().submit(() -> this.handleInner(transport));

    }

    private void handleInner(ITransport transport) {


    }


}
