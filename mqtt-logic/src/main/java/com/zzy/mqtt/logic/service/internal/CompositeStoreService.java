package com.zzy.mqtt.logic.service.internal;


import cn.hutool.core.collection.CollectionUtil;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.zzy.mqtt.logic.MqttLogic;
import com.zzy.mqtt.logic.config.MqttLogicConfig;
import com.zzy.mqtt.logic.service.store.*;
import com.zzy.mqtt.logic.service.transport.IClientStoreService;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

@Slf4j
@RequiredArgsConstructor
public class CompositeStoreService {

    private final IClientPublishMessageStoreService clientPublishMessageStoreService;
    private final IServerPublishMessageStoreService serverPublishMessageStoreService;
    private final ISubscribeStoreService subscribeStoreService;
    private final IMessageIdService messageIdService;

    private final ISessionStoreService sessionStoreService;

    private final IClientStoreService clientStoreService;
    private final IInternalMessageService internalMessageService;
    private final IRetainMessageStoreService retainMessageStoreService;
    private final MqttLogicConfig mqttLogicConfig;


    private final Cache<String, List<SubscribeStoreDTO>> subscribeCache =
            Caffeine.newBuilder()
                    .maximumSize(1000)
                    .expireAfterWrite(Duration.ofMillis(5000))
                    .build();


    @SneakyThrows
    private Future<Void> wrapSemaphoreTask(Callable<Void> task) {

        return MqttLogic.getStoreService().submit(() -> task.call());
    }

    @SneakyThrows
    private <T extends Object> Future<T> wrapSemaphoreTaskWithValue(Callable<T> task) {

        return MqttLogic.getStoreService().submit(() -> task.call());
    }

    @SneakyThrows
    public Future<Void> putRetainStore(String topic, RetainMessageStoreDTO storeDTO) {
        return wrapSemaphoreTask(() -> {
            retainMessageStoreService.put(topic, storeDTO);
            return null;
        });

    }

    @SneakyThrows
    public Future<Integer> getNextMessageId(String clientId) {
        return wrapSemaphoreTaskWithValue(() -> {
            messageIdService.getNextMessageId(clientId);
            return null;
        });

    }

    @SneakyThrows
    public Future<List<RetainMessageStoreDTO>> searchRetainStore(String topic) {
        return wrapSemaphoreTaskWithValue(() -> {
            retainMessageStoreService.search(topic);
            return null;
        });

    }

    @SneakyThrows
    public Future<SessionStoreDTO> getSessionStore(String clientId) {
        return wrapSemaphoreTaskWithValue(() -> sessionStoreService.get(clientId));

    }

    @SneakyThrows
    public Future<Void> putSessionStore(String clientId, SessionStoreDTO storeDTO) {
        return wrapSemaphoreTask(() -> {
            sessionStoreService.put(clientId, storeDTO);
            return null;
        });

    }

    @SneakyThrows
    public Future<Void> removeSessionStore(String clientId) {
        return wrapSemaphoreTask(() -> {
            sessionStoreService.remove(clientId);
            return null;
        });

    }

    public Future<Void> putClientStore(String clientId, String brokerId) {
        return wrapSemaphoreTask(() -> {
            clientStoreService.putClient(clientId, brokerId);
            return null;
        });
    }

    public Future<Void> removeClientStore(String clientId) {
        return wrapSemaphoreTask(() -> {
            clientStoreService.removeClient(clientId);
            return null;
        });
    }

    @SneakyThrows
    public Future<Void> removeSubscribeStoreForClient(String clientId) {
        return wrapSemaphoreTask(() -> {
            subscribeStoreService.removeForClient(clientId);
            return null;
        });

    }

    @SneakyThrows
    public Future<Void> putSubscribeStore(String topic, SubscribeStoreDTO storeDTO) {
        return wrapSemaphoreTask(() -> {
            subscribeStoreService.put(topic, storeDTO);
            return null;
        });

    }

    @SneakyThrows
    public Future<Void> removeSubscribeStore(String topic, String clientId) {
        return wrapSemaphoreTask(() -> {
            subscribeStoreService.remove(topic, clientId);
            return null;
        });

    }


    @SneakyThrows
    public Future<Void> storeClientPublishMessage(ClientPublishMessageStoreDTO dto) {
        log.debug("Storing client publish message: {}", dto);
        return wrapSemaphoreTask(() -> {
            clientPublishMessageStoreService.put(dto.getClientId(), dto);
            return null;
        });

    }

    public Future<Void> storeClientPublishMessageAndSend(ClientPublishMessageStoreDTO dto) {
        log.debug("Storing client publish message: {}", dto);
        return wrapSemaphoreTask(() -> {
            clientPublishMessageStoreService.put(dto.getClientId(), dto);
            storeServerPublishMessageAndSend(dto).get();
            clientPublishMessageStoreService.remove(dto.getClientId(), dto.getMessageId());
            return null;
        });
    }

    public Future<Void> removeServerPublishStore(String clientId, int messageId) {
        return wrapSemaphoreTask(() -> {
            serverPublishMessageStoreService.remove(clientId, messageId);
            return null;
        });
    }

    public Future<Void> storeServerPublishMessageAndSendByClientPublishMessage(String clientId, int messageId) {
        return wrapSemaphoreTask(() -> {
            ClientPublishMessageStoreDTO clientPublishMessageStoreDTO =
                    clientPublishMessageStoreService.get(clientId, messageId);
            clientPublishMessageStoreDTO.setHandshakeOk(true);
            clientPublishMessageStoreService.put(clientId, clientPublishMessageStoreDTO);
            log.debug("Storing server publish message and sending by client publish message: {}, {}", clientId, messageId);
            storeServerPublishMessageAndSend(clientPublishMessageStoreDTO).get();
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


        return wrapSemaphoreTask(() -> {
            List<SubscribeStoreDTO> subscribeStoreDTOS = getSubscribeStoreDTOS(event.getTopic());
            if (subscribeStoreDTOS == null) return null;

            for (SubscribeStoreDTO subscribeStoreDTO : subscribeStoreDTOS) {
                InternalMessageDTO internalMessageDTO = InternalMessageDTO.builder()
                        .messageBytes(event.getMessageBytes())
                        .topic(subscribeStoreDTO.getTopicFilter())
                        .retain(false)
                        .messageId(messageIdService.getNextMessageId(subscribeStoreDTO.getClientId()))
                        .toClientId(subscribeStoreDTO.getClientId())
                        .mqttQoS(subscribeStoreDTO.getMqttQoS())
                        .dup(false)
                        .build();
                if (subscribeStoreDTO.getMqttQoS() > 0) {
                    ServerPublishMessageStoreDTO serverPublishMessageStoreDTO = ServerPublishMessageStoreDTO.builder()
                            .fromClientId(event.getClientId())
                            .clientId(subscribeStoreDTO.getClientId())
                            .topic(subscribeStoreDTO.getTopicFilter())
                            .mqttQoS(subscribeStoreDTO.getMqttQoS())
                            .messageId(internalMessageDTO.getMessageId())
                            .messageBytes(event.getMessageBytes())
                            .createTime(System.currentTimeMillis())
                            .build();
                    serverPublishMessageStoreService.put(subscribeStoreDTO.getClientId(), serverPublishMessageStoreDTO);
                }
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
            log.debug("No subscribers found for topic: {}", topic);
            return null;
        }
        subscribeCache.put(topic, subscribeStoreDTOS);
        return subscribeStoreDTOS;
    }


}
