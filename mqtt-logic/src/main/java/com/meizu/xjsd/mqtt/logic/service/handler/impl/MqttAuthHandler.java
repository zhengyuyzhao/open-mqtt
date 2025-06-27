package com.meizu.xjsd.mqtt.logic.service.handler.impl;

import com.meizu.xjsd.mqtt.logic.MqttLogic;
import com.meizu.xjsd.mqtt.logic.service.handler.ConnectHandler;
import com.meizu.xjsd.mqtt.logic.service.transport.ITransport;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

@RequiredArgsConstructor
public class MqttAuthHandler implements ConnectHandler<ITransport> {
    private final String brokerId;

    @SneakyThrows
    @Override
    public void handle(ITransport transport) {

        MqttLogic.getExecutorService().submit(() -> this.handleInner(transport));

    }

    private void handleInner(ITransport transport) {


    }


}
