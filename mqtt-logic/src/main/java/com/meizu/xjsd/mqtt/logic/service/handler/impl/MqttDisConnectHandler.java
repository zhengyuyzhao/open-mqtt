package com.meizu.xjsd.mqtt.logic.service.handler.impl;

import cn.hutool.core.util.StrUtil;
import com.meizu.xjsd.mqtt.logic.MqttLogic;
import com.meizu.xjsd.mqtt.logic.service.handler.DisConnectHandler;
import com.meizu.xjsd.mqtt.logic.service.internal.CompositePublishService;
import com.meizu.xjsd.mqtt.logic.service.internal.IInternalMessageService;
import com.meizu.xjsd.mqtt.logic.service.internal.InternalMessageDTO;
import com.meizu.xjsd.mqtt.logic.service.store.*;
import com.meizu.xjsd.mqtt.logic.service.transport.IClientStoreService;
import com.meizu.xjsd.mqtt.logic.service.transport.ITransport;
import com.meizu.xjsd.mqtt.logic.service.transport.ITransportLocalStoreService;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;

@AllArgsConstructor
public class MqttDisConnectHandler implements DisConnectHandler<ITransport> {
    private final ITransportLocalStoreService transportLocalStoreService;
    private final ISessionStoreService sessionStoreService;
    private final ISubscribeStoreService subscribeStoreService;
    private final IClientStoreService clientStoreService;
    private final CompositePublishService compositePublishService;

    @SneakyThrows
    @Override
    public void handle(ITransport transport) {

        MqttLogic.getConnectionService().submit(() -> {

            try {
                handleInner(transport);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

    }

    private void handleInner(ITransport transport) throws Exception {
        transportLocalStoreService.removeTransport(transport.clientIdentifier());
        SessionStoreDTO sessionStoreDTO = sessionStoreService.get(transport.clientIdentifier());
        if (sessionStoreDTO != null) {
            // 发送遗嘱消息
            if (sessionStoreDTO.getWillMessage() != null && StrUtil.isNotEmpty(sessionStoreDTO.getWillMessage().getWillTopic())) {

                ClientPublishMessageStoreDTO clientPublishMessageStoreDTO =
                        ClientPublishMessageStoreDTO.builder()
                                .clientId(transport.clientIdentifier())
                                .messageBytes(sessionStoreDTO.getWillMessage().getWillMessageBytes())
                                .mqttQoS(sessionStoreDTO.getWillMessage().getWillQos())
                                .topic(sessionStoreDTO.getWillMessage().getWillTopic())
                                .createTime(System.currentTimeMillis())
                                .isHandshakeOk(true) // 遗嘱消息通常不需要握手确认
                                .build();

                compositePublishService.storeClientPublishMessageAndSend(
                        clientPublishMessageStoreDTO
                );

            }

            // 如果是清除会话，则删除会话
            if (transport.isCleanSession()) {
                sessionStoreService.remove(transport.clientIdentifier());
            } else {
                // 更新会话的过期时间
                sessionStoreService.expire(transport.clientIdentifier(), transport.keepAliveTimeSeconds());
            }
        }
        if (transport.isCleanSession()) {
            subscribeStoreService.removeForClient(transport.clientIdentifier());
        }
        clientStoreService.removeClient(transport.clientIdentifier());
    }
}
