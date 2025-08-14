package com.zzy.mqtt.logic.schedule;

import cn.hutool.core.thread.ThreadFactoryBuilder;
import com.zzy.mqtt.logic.config.MqttLogicConfig;
import com.zzy.mqtt.logic.service.internal.CompositeStoreService;
import com.zzy.mqtt.logic.service.lock.IDistributeLock;
import com.zzy.mqtt.logic.service.store.*;
import com.zzy.mqtt.logic.service.transport.ITransport;
import com.zzy.mqtt.logic.service.transport.ITransportLocalStoreService;
import io.netty.handler.codec.mqtt.MqttQoS;
import lombok.extern.slf4j.Slf4j;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

@Slf4j
public class DupMessageRetryScheduleService {
    private final MqttLogicConfig mqttLogicConfig;
    private final IServerPublishMessageStoreService serverPublishMessageStoreService;

    private final IClientPublishMessageStoreService clientPublishMessageStoreService;

    private final CompositeStoreService compositeStoreService;
    private final ITransportLocalStoreService transportLocalStoreService;

    private final ISubscribeStoreService subscribeStoreService;

    private final IDistributeLock distributeLock;


    private final ScheduledExecutorService serverMessagescheduledExecutorService;
    private final ScheduledExecutorService clientMessagescheduledExecutorService;


    private final long resendDelay = 60000; // 重发延迟时间，单位毫秒
    private final int resendTimes = 3; // 重发次数

    public DupMessageRetryScheduleService(MqttLogicConfig mqttLogicConfig,
                                          IServerPublishMessageStoreService serverPublishMessageStoreService,
                                          IClientPublishMessageStoreService clientPublishMessageStoreService,
                                          CompositeStoreService compositeStoreService,
                                          ITransportLocalStoreService transportLocalStoreService,
                                          ISubscribeStoreService subscribeStoreService,
                                          IDistributeLock distributeLock) {
        this.mqttLogicConfig = mqttLogicConfig;
        this.serverPublishMessageStoreService = serverPublishMessageStoreService;
        this.clientPublishMessageStoreService = clientPublishMessageStoreService;
        this.transportLocalStoreService = transportLocalStoreService;
        this.compositeStoreService = compositeStoreService;
        this.subscribeStoreService = subscribeStoreService;
        this.distributeLock = distributeLock;
        this.serverMessagescheduledExecutorService = new ScheduledThreadPoolExecutor(
                mqttLogicConfig.getDupMessageRetryThreadPoolSize(),
                new ThreadFactoryBuilder().setNamePrefix("server-message-retry").build()
        );
        this.clientMessagescheduledExecutorService = new ScheduledThreadPoolExecutor(
                mqttLogicConfig.getDupMessageRetryThreadPoolSize(),
                new ThreadFactoryBuilder().setNamePrefix("client-message-retry").build()
        );
        start();

    }

    public void start() {
        Runnable serverTask = () -> {
            Iterator<ITransport> iterator = transportLocalStoreService.list().iterator();
            while (iterator.hasNext()) {
                ITransport transport = iterator.next();

                try {
                    if (!transport.isConnected()) {
                        transportLocalStoreService.removeTransport(transport.clientIdentifier());
                        continue;
                    }
                    sendDupMessage(transport);
                } catch (Exception e) {
                    log.error("Error during retrying duplicate publish messages for transport: {}", transport.clientIdentifier());
                }
            }
        };

        Runnable clientTask = () -> {
            Lock lock = distributeLock.getLock("client-dup-message-retry");
            try {
                lock.lock();
                // 发送客户端的重复发布消息
                sendClientDupMessage();
            } catch (Exception e) {
                log.error("Error during retrying duplicate client messages", e);
            } finally {
                lock.unlock();
            }
//            sendClientDupMessage();

        };
        serverMessagescheduledExecutorService.scheduleWithFixedDelay(serverTask,
                mqttLogicConfig.getDupMessageRetryInitialDelay(),
                mqttLogicConfig.getDupMessageRetryDelay(), TimeUnit.MILLISECONDS);
        clientMessagescheduledExecutorService.scheduleWithFixedDelay(clientTask,
                mqttLogicConfig.getDupMessageRetryInitialDelay(),
                mqttLogicConfig.getDupMessageRetryDelay(), TimeUnit.MILLISECONDS);
    }

    public void stop() {
        if (serverMessagescheduledExecutorService != null && !serverMessagescheduledExecutorService.isShutdown()) {
            serverMessagescheduledExecutorService.shutdown();
        }
        if (clientMessagescheduledExecutorService != null && !clientMessagescheduledExecutorService.isShutdown()) {
            clientMessagescheduledExecutorService.shutdown();
        }
    }

    private void sendDupMessage(ITransport transport) {
        // 发送重复发布的消息
        List<ServerPublishMessageStoreDTO> messages = serverPublishMessageStoreService.get(transport.clientIdentifier());
        log.debug("Retrying duplicate publish messages for transport: {}, messages: {}", transport.clientIdentifier(), messages);
        if (messages != null && !messages.isEmpty()) {
            messages.forEach(message -> {
                log.debug("Checking message: {}, createTime: {}, times: {}", message.getMessageId(), message.getCreateTime(), message.getTimes());
                if (serverPublishMessageStoreService.get(transport.clientIdentifier(), message.getMessageId()) == null) {
                    return;
                }

                if (System.currentTimeMillis() - message.getCreateTime() < resendDelay) {
                    return;
                }

                if (message.getTimes() > resendTimes) {
                    log.warn("Message {} has been retried {} times, removing from store", message.getMessageId(), message.getTimes());
                    serverPublishMessageStoreService.remove(transport.clientIdentifier(), message.getMessageId());
                    return;
                }


                log.debug("Sending duplicate message: {}, topic: {}, qos: {}, messageId: {}",
                        message.getMessageId(), message.getTopic(), message.getMqttQoS(), message.getMessageId());
                ITransport currentTransport = transportLocalStoreService.getTransport(transport.clientIdentifier());
                if (currentTransport == null) {
                    return;
                }
//                ClientPublishMessageStoreDTO clientPublishMessageStoreDTO = clientPublishMessageStoreService
//                        .get(message.getFromClientId(), message.getMessageId());
//                if (clientPublishMessageStoreDTO == null) {
//                    serverPublishMessageStoreService.remove(transport.clientIdentifier(), message.getMessageId());
//                    return;
//                }

//                SubscribeStoreDTO subscribeStoreDTO = subscribeStoreService.get(message.getTopic(), currentTransport.clientIdentifier());
//                if (subscribeStoreDTO == null || subscribeStoreDTO.getMqttQoS() == 0) {
//                    serverPublishMessageStoreService.remove(transport.clientIdentifier(), message.getMessageId());
//                    return;
//                }

                message.setCreateTime(System.currentTimeMillis());
                message.setTimes(message.getTimes() + 1);
                serverPublishMessageStoreService.put(transport.clientIdentifier(), message);

                currentTransport.publish(message.getTopic(), message.getMessageBytes(),
                        MqttQoS.valueOf(message.getMqttQoS()), true, false, message.getMessageId());
            });
        }
    }

    private void sendClientDupMessage() {
        // 发送重复发布的消息
        List<ClientPublishMessageStoreDTO> messages = clientPublishMessageStoreService.getAll();
        if (messages != null && !messages.isEmpty()) {
            messages.forEach(message -> {
                if (!message.isHandshakeOk()) {
                    return;
                }
                if (System.currentTimeMillis() - message.getCreateTime() < resendDelay) {
                    return;
                }

                if (clientPublishMessageStoreService.get(message.getClientId(), message.getMessageId()) == null) {
                    return;
                }
                log.debug("Sending duplicate client message: {}, topic: {}, qos: {}, messageId: {}",
                        message.getMessageId(), message.getTopic(), message.getMqttQoS(), message.getMessageId());
                try {
                    compositeStoreService.storeServerPublishMessageAndSend(message, true).get();
                    clientPublishMessageStoreService.remove(message.getClientId(), message.getMessageId());
                } catch (Exception e) {
                    log.error("Error sending duplicate client message: {}, topic: {}, qos: {}, messageId: {}",
                            message.getMessageId(), message.getTopic(), message.getMqttQoS(), message.getMessageId());
                }


            });

        }
    }
}
