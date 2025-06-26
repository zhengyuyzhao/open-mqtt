package com.meizu.xjsd.mqtt.logic.schedule;

import cn.hutool.core.thread.ThreadFactoryBuilder;
import com.meizu.xjsd.mqtt.logic.config.MqttLogicConfig;
import com.meizu.xjsd.mqtt.logic.service.store.DupPublishMessageStoreDTO;
import com.meizu.xjsd.mqtt.logic.service.store.IDupPublishMessageStoreService;
import com.meizu.xjsd.mqtt.logic.service.transport.ITransport;
import com.meizu.xjsd.mqtt.logic.service.transport.ITransportLocalStoreService;
import io.netty.handler.codec.mqtt.MqttQoS;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Slf4j
public class DupMessageRetryScheduleService {
    private final MqttLogicConfig mqttLogicConfig;
    private final IDupPublishMessageStoreService dupPublishMessageStoreService;
    private final ITransportLocalStoreService transportLocalStoreService;


    private final ScheduledExecutorService scheduledExecutorService;

    public DupMessageRetryScheduleService(MqttLogicConfig mqttLogicConfig, IDupPublishMessageStoreService dupPublishMessageStoreService, ITransportLocalStoreService transportLocalStoreService) {
        this.mqttLogicConfig = mqttLogicConfig;
        this.dupPublishMessageStoreService = dupPublishMessageStoreService;
        this.transportLocalStoreService = transportLocalStoreService;
        this.scheduledExecutorService = new ScheduledThreadPoolExecutor(
                mqttLogicConfig.getDupMessageRetryThreadPoolSize(),
                new ThreadFactoryBuilder().setNamePrefix("dup-message-retry").build()
        );
        start();

    }

    public void start() {
        Runnable task = () -> {
            transportLocalStoreService.list().forEach(transport -> {
                try {
                    sendDupMessage(transport);
                } catch (Exception e) {
                    log.error("Error during retrying duplicate publish messages for transport: {}", transport.clientIdentifier(), e);
                }
            });

        };
        scheduledExecutorService.scheduleWithFixedDelay(task,
                mqttLogicConfig.getDupMessageRetryInitialDelay(),
                mqttLogicConfig.getDupMessageRetryDelay(), TimeUnit.SECONDS);
    }

    public void stop() {
        if (scheduledExecutorService != null && !scheduledExecutorService.isShutdown()) {
            scheduledExecutorService.shutdown();
        }
    }

    private void sendDupMessage(ITransport transport) {
        // 发送重复发布的消息
        List<DupPublishMessageStoreDTO> messages = dupPublishMessageStoreService.get(transport.clientIdentifier());
        if (messages != null && !messages.isEmpty()) {
            messages.forEach(message -> {
                if (message.getTimes() > mqttLogicConfig.getDupMessageRetryMaxTimes()) {
                    log.warn("Dup publish message retry times exceeded for clientId: {}, messageId: {}",
                            transport.clientIdentifier(), message.getMessageId());
                    return;
                }
                transport.publish(message.getTopic(), message.getMessageBytes(),
                        MqttQoS.valueOf(message.getMqttQoS()), true, false, message.getMessageId());
            });
        }
    }
}
