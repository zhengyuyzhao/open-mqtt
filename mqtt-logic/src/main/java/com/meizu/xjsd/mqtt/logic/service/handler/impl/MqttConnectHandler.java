package com.meizu.xjsd.mqtt.logic.service.handler.impl;

import com.meizu.xjsd.mqtt.logic.MqttLogic;
import com.meizu.xjsd.mqtt.logic.entity.IMqttAuth;
import com.meizu.xjsd.mqtt.logic.entity.MqttWill;
import com.meizu.xjsd.mqtt.logic.service.auth.IAuthService;
import com.meizu.xjsd.mqtt.logic.service.handler.ConnectHandler;
import com.meizu.xjsd.mqtt.logic.service.store.DupPublishMessageStoreDTO;
import com.meizu.xjsd.mqtt.logic.service.store.IDupPublishMessageStoreService;
import com.meizu.xjsd.mqtt.logic.service.store.ISessionStoreService;
import com.meizu.xjsd.mqtt.logic.service.store.SessionStoreDTO;
import com.meizu.xjsd.mqtt.logic.service.transport.ITransport;
import com.meizu.xjsd.mqtt.logic.service.transport.ITransportLocalStoreService;
import io.netty.handler.codec.mqtt.MqttConnectReturnCode;
import io.netty.handler.codec.mqtt.MqttQoS;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class MqttConnectHandler implements ConnectHandler<ITransport> {
    private final IAuthService authService;
    private final ITransportLocalStoreService transportLocalStoreService;
    private final ISessionStoreService sessionStoreService;
    private final IDupPublishMessageStoreService dupPublishMessageStoreService;


    @Override
    public void handle(ITransport transport) {
        MqttLogic.getExecutorService().execute(() -> this.handleInner(transport));
    }

    private void handleInner(ITransport transport) {
        IMqttAuth auth = transport.auth();
//        if (auth == null || auth.getUsername() == null || auth.getPassword() == null) {
//            transport.reject(transport.protocolVersion() < 5 ? MqttConnectReturnCode.CONNECTION_REFUSED_BAD_USER_NAME_OR_PASSWORD
//                    : MqttConnectReturnCode.CONNECTION_REFUSED_UNSPECIFIED_ERROR);
//            return;
//        }
        if (!authService.checkValid(auth)) {
            transport.reject(transport.protocolVersion() < 5 ? MqttConnectReturnCode.CONNECTION_REFUSED_BAD_USER_NAME_OR_PASSWORD
                    : MqttConnectReturnCode.CONNECTION_REFUSED_UNSPECIFIED_ERROR);
            return;
        }
        ITransport existingTransport = transportLocalStoreService.getTransport(transport.clientIdentifier());
        if (existingTransport != null) {
            transportLocalStoreService.removeTransport(existingTransport.clientIdentifier());
            existingTransport.close();
        }
        transportLocalStoreService.putTransport(transport.clientIdentifier(), transport);
        SessionStoreDTO sessionStoreDTO = sessionStoreService.get(transport.clientIdentifier());
        if (transport.isCleanSession() || sessionStoreDTO == null) {
            sessionStoreDTO = SessionStoreDTO.builder()
                    .clientId(transport.clientIdentifier())
                    .expire(transport.keepAliveTimeSeconds())
                    .cleanSession(transport.isCleanSession())
                    .willMessage(MqttWill.of(transport.will()))
                    .build();
            sessionStoreService.put(transport.clientIdentifier(), sessionStoreDTO);
        }
        sendDupMessage(transport);

    }

    private void sendDupMessage(ITransport transport) {
        // 发送重复发布的消息
        List<DupPublishMessageStoreDTO> messages = dupPublishMessageStoreService.get(transport.clientIdentifier());
        if (messages != null && !messages.isEmpty()) {
            messages.forEach(message -> {
                transport.publish(message.getTopic(), message.getMessageBytes(),
                        MqttQoS.valueOf(message.getMqttQoS()), true, false, message.getMessageId());
            });
        }
    }


}
