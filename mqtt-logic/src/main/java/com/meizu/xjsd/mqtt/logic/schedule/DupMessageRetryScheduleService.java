package com.meizu.xjsd.mqtt.logic.schedule;

import cn.hutool.core.thread.ThreadFactoryBuilder;
import com.meizu.xjsd.mqtt.logic.config.MqttLogicConfig;
import com.meizu.xjsd.mqtt.logic.service.internal.CompositePublishService;
import com.meizu.xjsd.mqtt.logic.service.store.ClientPublishMessageStoreDTO;
import com.meizu.xjsd.mqtt.logic.service.store.IClientPublishMessageStoreService;
import com.meizu.xjsd.mqtt.logic.service.store.IServerPublishMessageStoreService;
import com.meizu.xjsd.mqtt.logic.service.store.ServerPublishMessageStoreDTO;
import com.meizu.xjsd.mqtt.logic.service.transport.ITransport;
import com.meizu.xjsd.mqtt.logic.service.transport.ITransportLocalStoreService;
import io.netty.handler.codec.mqtt.MqttQoS;
import lombok.extern.slf4j.Slf4j;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Slf4j
public class DupMessageRetryScheduleService {
    private final MqttLogicConfig mqttLogicConfig;
    private final IServerPublishMessageStoreService serverPublishMessageStoreService;

    private final IClientPublishMessageStoreService clientPublishMessageStoreService;

    private final CompositePublishService compositePublishService;
    private final ITransportLocalStoreService transportLocalStoreService;


    private final ScheduledExecutorService serverMessagescheduledExecutorService;
    private final ScheduledExecutorService clientMessagescheduledExecutorService;

    private final long resendDelay = 15000; // 重发延迟时间，单位毫秒

    public DupMessageRetryScheduleService(MqttLogicConfig mqttLogicConfig,
                                          IServerPublishMessageStoreService serverPublishMessageStoreService,
                                          IClientPublishMessageStoreService clientPublishMessageStoreService,
                                          CompositePublishService compositePublishService,
                                          ITransportLocalStoreService transportLocalStoreService) {
        this.mqttLogicConfig = mqttLogicConfig;
        this.serverPublishMessageStoreService = serverPublishMessageStoreService;
        this.clientPublishMessageStoreService = clientPublishMessageStoreService;
        this.transportLocalStoreService = transportLocalStoreService;
        this.compositePublishService = compositePublishService;
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
                    log.error("Error during retrying duplicate publish messages for transport: {}", transport.clientIdentifier(), e);
                }
            }
        };

        Runnable clientTask = () -> {
            Iterator<ITransport> iterator = transportLocalStoreService.list().iterator();
            while (iterator.hasNext()) {
                ITransport transport = iterator.next();

                try {
                    sendClientDupMessage(transport);
                } catch (Exception e) {
                    log.error("Error during retrying duplicate publish messages for transport: {}", transport.clientIdentifier(), e);
                }
            }
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
                if (serverPublishMessageStoreService.get(transport.clientIdentifier(), message.getMessageId()) == null) {
                    return;
                }

                if (System.currentTimeMillis() - message.getCreateTime() < resendDelay) {
                    return;
                }

                log.info("Sending duplicate message: {}, topic: {}, qos: {}, messageId: {}",
                        message.getMessageId(), message.getTopic(), message.getMqttQoS(), message.getMessageId());
                ITransport currentTransport = transportLocalStoreService.getTransport(transport.clientIdentifier());
                if (currentTransport == null) {
                    return;
                }
                ClientPublishMessageStoreDTO clientPublishMessageStoreDTO = clientPublishMessageStoreService
                        .get(message.getFromClientId(), message.getMessageId());
                if (clientPublishMessageStoreDTO == null) {
                    return;
                }

                currentTransport.publish(message.getTopic(), clientPublishMessageStoreDTO.getMessageBytes(),
                        MqttQoS.valueOf(message.getMqttQoS()), true, false, message.getMessageId());
            });
        }
    }

    private void sendClientDupMessage(ITransport transport) {
        // 发送重复发布的消息
        List<ClientPublishMessageStoreDTO> messages = clientPublishMessageStoreService.get(transport.clientIdentifier());
        log.debug("Retrying duplicate publish messages for transport: {}, messages: {}", transport.clientIdentifier(), messages);
        if (messages != null && !messages.isEmpty()) {
            messages.forEach(message -> {
                if (!message.isHandshakeOk()) {
                    return;
                }
                if (System.currentTimeMillis() - message.getCreateTime() < resendDelay) {
                    return;
                }

                if (clientPublishMessageStoreService.get(transport.clientIdentifier(), message.getMessageId()) == null) {
                    return;
                }
                log.info("Sending duplicate client message: {}, topic: {}, qos: {}, messageId: {}",
                        message.getMessageId(), message.getTopic(), message.getMqttQoS(), message.getMessageId());
                compositePublishService.storeServerPublishMessageAndSend(message);
                clientPublishMessageStoreService.remove(message.getClientId(), message.getMessageId());

            });

        }
    }
}
