package com.zzy.mqtt.logic.service.handler.impl;

import com.zzy.mqtt.logic.MqttLogic;
import com.zzy.mqtt.logic.entity.IMqttAuth;
import com.zzy.mqtt.logic.entity.MqttWill;
import com.zzy.mqtt.logic.service.auth.IAuthService;
import com.zzy.mqtt.logic.service.handler.ConnectHandler;
import com.zzy.mqtt.logic.service.store.ClientPublishMessageStoreDTO;
import com.zzy.mqtt.logic.service.store.IServerPublishMessageStoreService;
import com.zzy.mqtt.logic.service.store.ISessionStoreService;
import com.zzy.mqtt.logic.service.store.SessionStoreDTO;
import com.zzy.mqtt.logic.service.transport.IClientStoreService;
import com.zzy.mqtt.logic.service.transport.ITransport;
import com.zzy.mqtt.logic.service.transport.ITransportLocalStoreService;
import io.netty.handler.codec.mqtt.MqttConnectReturnCode;
import io.netty.handler.codec.mqtt.MqttQoS;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class MqttConnectHandler implements ConnectHandler<ITransport> {
    private final IAuthService authService;
    private final ITransportLocalStoreService transportLocalStoreService;
    private final ISessionStoreService sessionStoreService;
    private final IServerPublishMessageStoreService dupPublishMessageStoreService;
    private final IClientStoreService clientStoreService;
    private final String brokerId;


    @SneakyThrows
    @Override
    public void handle(ITransport transport) {

        MqttLogic.getConnectionService().execute(() -> this.handleInner(transport));

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
            try {
                transportLocalStoreService.removeTransport(existingTransport.clientIdentifier());
                existingTransport.close();
            } catch (Exception e) {
                log.error("Error closing existing transport for clientId: {}", transport.clientIdentifier());
            }
        }
        SessionStoreDTO sessionStoreDTO = sessionStoreService.get(transport.clientIdentifier());
        transport.accept(sessionStoreDTO != null);


        if (transport.isCleanSession() || sessionStoreDTO == null) {
            sessionStoreDTO = SessionStoreDTO.builder()
                    .clientId(transport.clientIdentifier())
                    .expire(transport.keepAliveTimeSeconds())
                    .cleanSession(transport.isCleanSession())
                    .willMessage(MqttWill.of(transport.will()))
                    .build();
            sessionStoreService.put(transport.clientIdentifier(), sessionStoreDTO);
        }
//        sendDupMessage(transport);

        clientStoreService.putClient(transport.clientIdentifier(), brokerId);
        transportLocalStoreService.putTransport(transport.clientIdentifier(), transport);

    }

//    private void sendDupMessage(ITransport transport) {
//        // 发送重复发布的消息
//        List<ClientPublishMessageStoreDTO> messages = dupPublishMessageStoreService.get(transport.clientIdentifier());
//        log.debug("Sending duplicate messages for clientId: {}, messages: {}", transport.clientIdentifier(), messages);
//        if (messages != null && !messages.isEmpty()) {
//            messages.forEach(message -> {
//                transport.publish(message.getTopic(), message.getMessageBytes(),
//                        MqttQoS.valueOf(message.getMqttQoS()), true, false, message.getMessageId());
//            });
//        }
//    }


}
