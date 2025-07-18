package com.zzy.mqtt.logic.service.handler.impl;

import cn.hutool.core.util.StrUtil;
import com.zzy.mqtt.logic.MqttLogic;
import com.zzy.mqtt.logic.service.handler.DisConnectHandler;
import com.zzy.mqtt.logic.service.internal.CompositePublishService;

import com.zzy.mqtt.logic.service.store.ClientPublishMessageStoreDTO;
import com.zzy.mqtt.logic.service.store.ISessionStoreService;
import com.zzy.mqtt.logic.service.store.ISubscribeStoreService;
import com.zzy.mqtt.logic.service.store.SessionStoreDTO;
import com.zzy.mqtt.logic.service.transport.IClientStoreService;
import com.zzy.mqtt.logic.service.transport.ITransport;
import com.zzy.mqtt.logic.service.transport.ITransportLocalStoreService;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;


@Slf4j
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
        log.info("处理断开连接, clientId: {}, cleanSession: {}",
                transport.clientIdentifier(), transport.isCleanSession());
        MqttLogic.getConnectionService().submit(() -> {

            try {
                handleInner(transport);
            } catch (Exception e) {
                log.error("处理断开连接异常, clientId: {}, error: {}", transport.clientIdentifier(), e.getMessage());
            }
        });

    }

    private void handleInner(ITransport transport) {
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
