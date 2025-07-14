package com.zzy.mqtt.logic.service.internal;


import cn.hutool.core.collection.CollectionUtil;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.zzy.mqtt.logic.MqttLogic;
import com.zzy.mqtt.logic.service.store.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.Future;

@Slf4j
@RequiredArgsConstructor
public class CompositePublishService {

    private final IClientPublishMessageStoreService clientPublishMessageStoreService;
    private final IServerPublishMessageStoreService serverPublishMessageStoreService;
    private final ISubscribeStoreService subscribeStoreService;
    private final IMessageIdService messageIdService;
    private final IInternalMessageService internalMessageService;
    private final Cache<String, List<SubscribeStoreDTO>> subscribeCache =
            Caffeine.newBuilder()
                    .maximumSize(1000)
                    .expireAfterWrite(Duration.ofMillis(5000))
                    .build();

    public Future<Void> storeClientPublishMessage(ClientPublishMessageStoreDTO dto) {
        log.info("Storing client publish message: {}", dto);
        return MqttLogic.getStoreService().submit(() -> {
            clientPublishMessageStoreService.put(dto.getClientId(), dto);
            return null;
        });
    }

    public Future<Void> storeClientPublishMessageAndSend(ClientPublishMessageStoreDTO dto) {
        log.info("Storing client publish message: {}", dto);
        return MqttLogic.getStoreService().submit(() -> {
            clientPublishMessageStoreService.put(dto.getClientId(), dto);
            storeServerPublishMessageAndSend(dto);
            clientPublishMessageStoreService.remove(dto.getClientId(), dto.getMessageId());
            return null;
        });
    }

    public Future<Void> storeServerPublishMessageAndSendByClientPublishMessage(String clientId, int messageId) {
        return MqttLogic.getStoreService().submit(() -> {
            ClientPublishMessageStoreDTO clientPublishMessageStoreDTO =
                    clientPublishMessageStoreService.get(clientId, messageId);
            clientPublishMessageStoreDTO.setHandshakeOk(true);
            clientPublishMessageStoreService.put(clientId, clientPublishMessageStoreDTO);
            log.info("Storing server publish message and sending by client publish message: {}, {}", clientId, messageId);
            storeServerPublishMessageAndSend(clientPublishMessageStoreDTO);
            clientPublishMessageStoreService.remove(clientId, messageId);
            return null;
        });


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

    public Future<Void> storeServerPublishMessageAndSend(ClientPublishMessageStoreDTO event) {


        return MqttLogic.getStoreService().submit(() -> {
            List<SubscribeStoreDTO> subscribeStoreDTOS = getSubscribeStoreDTOS(event.getTopic());
            if (subscribeStoreDTOS == null) return null;

            for (SubscribeStoreDTO subscribeStoreDTO : subscribeStoreDTOS) {
                InternalMessageDTO internalMessageDTO = InternalMessageDTO.builder()
                        .messageBytes(event.getMessageBytes())
                        .topic(event.getTopic())
                        .retain(false)
                        .messageId(messageIdService.getNextMessageId(subscribeStoreDTO.getClientId()))
                        .toClientId(subscribeStoreDTO.getClientId())
                        .mqttQoS(subscribeStoreDTO.getMqttQoS())
                        .dup(false)
                        .build();
                ServerPublishMessageStoreDTO serverPublishMessageStoreDTO = ServerPublishMessageStoreDTO.builder()
                        .fromClientId(event.getClientId())
                        .clientId(subscribeStoreDTO.getClientId())
                        .topic(event.getTopic())
                        .mqttQoS(subscribeStoreDTO.getMqttQoS())
                        .messageId(internalMessageDTO.getMessageId())
                        .createTime(System.currentTimeMillis())
                        .build();
                serverPublishMessageStoreService.put(subscribeStoreDTO.getClientId(), serverPublishMessageStoreDTO);
                internalMessageService.internalPublish(internalMessageDTO);
            }
            return null;
        });

    }


    private List<SubscribeStoreDTO> getSubscribeStoreDTOS(String topic) {
        List<SubscribeStoreDTO> cache = subscribeCache.getIfPresent(topic);
        if (cache != null) {
            return cache;
        }
        List<SubscribeStoreDTO> subscribeStoreDTOS = subscribeStoreService.search(topic);
        if (CollectionUtil.isEmpty(subscribeStoreDTOS)) {
            log.info("No subscribers found for topic: {}", topic);
            return null;
        }
        subscribeCache.put(topic, subscribeStoreDTOS);
        return subscribeStoreDTOS;
    }

    public Future<Void> send(ClientPublishMessageStoreDTO event) {
        return MqttLogic.getStoreService().submit(() -> {
            List<SubscribeStoreDTO> subscribeStoreDTOS = getSubscribeStoreDTOS(event.getTopic());
            if (subscribeStoreDTOS == null) return null;

            for (SubscribeStoreDTO subscribeStoreDTO : subscribeStoreDTOS) {
                InternalMessageDTO internalMessageDTO = InternalMessageDTO.builder()
                        .messageBytes(event.getMessageBytes())
                        .topic(event.getTopic())
                        .retain(false)
                        .messageId(messageIdService.getNextMessageId(subscribeStoreDTO.getClientId()))
                        .toClientId(subscribeStoreDTO.getClientId())
                        .mqttQoS(subscribeStoreDTO.getMqttQoS())
                        .dup(false)
                        .build();
                internalMessageService.internalPublish(internalMessageDTO);
            }
            return null;
        });

    }


}
