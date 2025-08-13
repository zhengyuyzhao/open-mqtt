package com.zzy.mqtt.logic.service.handler.impl;

import com.zzy.mqtt.logic.MqttLogic;
import com.zzy.mqtt.logic.entity.IMqttAuth;
import com.zzy.mqtt.logic.entity.MqttWill;
import com.zzy.mqtt.logic.service.auth.IAuthService;
import com.zzy.mqtt.logic.service.handler.ConnectHandler;
import com.zzy.mqtt.logic.service.internal.CompositeStoreService;
import com.zzy.mqtt.logic.service.store.SessionStoreDTO;
import com.zzy.mqtt.logic.service.transport.ITransport;
import com.zzy.mqtt.logic.service.transport.ITransportLocalStoreService;
import io.netty.handler.codec.mqtt.MqttConnectReturnCode;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class MqttConnectHandler implements ConnectHandler<ITransport> {
    private final IAuthService authService;
    private final ITransportLocalStoreService transportLocalStoreService;
    private final CompositeStoreService compositeStoreService;
    private final String brokerId;


    @SneakyThrows
    @Override
    public void handle(ITransport transport) {

        MqttLogic.getConnectService().execute(() ->
                {
                    try{
                        // Call the inner handling logic
                        handleInner(transport);
                    } catch (Exception e) {
                        log.error("Error handling MQTT Connect: {}", transport.clientIdentifier(), e);
                        // Handle the exception appropriately, maybe send an error response or log it
                        transport.reject(transport.protocolVersion() < 5 ? MqttConnectReturnCode.CONNECTION_REFUSED_UNACCEPTABLE_PROTOCOL_VERSION
                                : MqttConnectReturnCode.CONNECTION_REFUSED_UNSPECIFIED_ERROR);
                    }
//                    this.handleInner(transport)
                }
        );

    }

    @SneakyThrows
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
        SessionStoreDTO sessionStoreDTO = compositeStoreService
                .getSessionStore(transport.clientIdentifier()).get();

        transport.accept(sessionStoreDTO != null);
        transportLocalStoreService.putTransport(transport.clientIdentifier(), transport);


        if (transport.isCleanSession() || sessionStoreDTO == null) {
            sessionStoreDTO = SessionStoreDTO.builder()
                    .clientId(transport.clientIdentifier())
                    .expire(transport.keepAliveTimeSeconds())
                    .cleanSession(transport.isCleanSession())
                    .willMessage(MqttWill.of(transport.will()))
                    .build();
            compositeStoreService.putSessionStore(transport.clientIdentifier(), sessionStoreDTO).get();
        }
//        sendDupMessage(transport);

        compositeStoreService.putClientStore(transport.clientIdentifier(), brokerId).get();


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
