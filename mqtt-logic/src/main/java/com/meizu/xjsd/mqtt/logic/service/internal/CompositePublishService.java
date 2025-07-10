package com.meizu.xjsd.mqtt.logic.service.internal;

import com.meizu.xjsd.mqtt.logic.service.store.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class CompositePublishService {

    private final IClientPublishMessageStoreService clientPublishMessageStoreService;
    private final IServerPublishMessageStoreService serverPublishMessageStoreService;
    private final ISubscribeStoreService subscribeStoreService;
    private final IMessageIdService messageIdService;
    private final IInternalMessageService internalMessageService;

    public void storeClientPublishMessage(ClientPublishMessageStoreDTO dto) {
        log.info("Storing client publish message: {}", dto);
        clientPublishMessageStoreService.put(dto.getClientId(), dto);
    }

    public void storeClientPublishMessageAndSend(ClientPublishMessageStoreDTO dto) {
        log.info("Storing client publish message: {}", dto);
        clientPublishMessageStoreService.put(dto.getClientId(), dto);
        storeServerPublishMessageAndSend(dto);
        clientPublishMessageStoreService.remove(dto.getClientId(), dto.getMessageId());
    }

    public void storeServerPublishMessageAndSendByClientPublishMessage(String clientId, int messageId) {
        ClientPublishMessageStoreDTO clientPublishMessageStoreDTO =
                clientPublishMessageStoreService.get(clientId, messageId);
        clientPublishMessageStoreDTO.setHandshakeOk(true);
        clientPublishMessageStoreService.put(clientId, clientPublishMessageStoreDTO);
        log.info("Storing server publish message and sending by client publish message: {}, {}", clientId, messageId);
        storeServerPublishMessageAndSend(clientPublishMessageStoreDTO);
        clientPublishMessageStoreService.remove(clientId, messageId);

    }

//    public void storeServerPublishMessageAndSendDirect(ServerPublishMessageStoreDTO event) {
//        List<SubscribeStoreDTO> list = subscribeStoreService.search(event.getTopic());
//        list.forEach(subscribeStoreDTO -> {
//            int messageId = messageIdService.getNextMessageId(subscribeStoreDTO.getClientId());
//            InternalMessageDTO internalMessageDTO = InternalMessageDTO.builder()
//                    .toClientId(subscribeStoreDTO.getClientId())
//                    .messageBytes(event.getMessageBytes())
//                    .topic(event.getTopic())
//                    .mqttQoS(event.getMqttQoS())
//                    .messageId(messageId)
//                    .retain(false)
//                    .dup(false)
//                    .build();
//            internalMessageService.internalPublish(internalMessageDTO);
//        });
//    }

    public void storeServerPublishMessageAndSend(ClientPublishMessageStoreDTO event) {

        InternalMessageDTO internalMessageDTO = InternalMessageDTO.builder()
//                    .toClientId(subscribeStoreDTO.getClientId())
                .messageBytes(event.getMessageBytes())
                .topic(event.getTopic())
//                    .mqttQoS(subscribeStoreDTO.getMqttQoS())
//                .messageId(messageId)
                .retain(false)
                .dup(false)
                .build();
        internalMessageService.internalPublish(internalMessageDTO);
    }

}
