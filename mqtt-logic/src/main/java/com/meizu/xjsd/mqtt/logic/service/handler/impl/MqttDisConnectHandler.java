package com.meizu.xjsd.mqtt.logic.service.handler.impl;

import cn.hutool.core.util.StrUtil;
import com.meizu.xjsd.mqtt.logic.MqttLogic;
import com.meizu.xjsd.mqtt.logic.service.handler.DisConnectHandler;
import com.meizu.xjsd.mqtt.logic.service.internal.IInternalMessageService;
import com.meizu.xjsd.mqtt.logic.service.internal.InternalMessageDTO;
import com.meizu.xjsd.mqtt.logic.service.store.ISessionStoreService;
import com.meizu.xjsd.mqtt.logic.service.store.ISubscribeStoreService;
import com.meizu.xjsd.mqtt.logic.service.store.SessionStoreDTO;
import com.meizu.xjsd.mqtt.logic.service.transport.ITransport;
import com.meizu.xjsd.mqtt.logic.service.transport.ITransportLocalStoreService;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;

@AllArgsConstructor
public class MqttDisConnectHandler implements DisConnectHandler<ITransport> {
    private final String brokerId;
    private final ITransportLocalStoreService transportLocalStoreService;
    private final ISessionStoreService sessionStoreService;
    private final ISubscribeStoreService subscribeStoreService;
    private final IInternalMessageService internalMessageService;

    @SneakyThrows
    @Override
    public void handle(ITransport transport) {

        MqttLogic.getExecutorService().submit(() -> {

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
                InternalMessageDTO internalMessageDTO = InternalMessageDTO.builder()
                        .topic(sessionStoreDTO.getWillMessage().getWillTopic())
                        .mqttQoS(sessionStoreDTO.getWillMessage().getWillQos())
                        .messageBytes(sessionStoreDTO.getWillMessage().getWillMessageBytes())
                        .dup(false)
                        .retain(sessionStoreDTO.getWillMessage().isWillRetain())
                        .clientId(transport.clientIdentifier())
                        .build();
                internalMessageService.internalPublish(internalMessageDTO);
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
    }
}
