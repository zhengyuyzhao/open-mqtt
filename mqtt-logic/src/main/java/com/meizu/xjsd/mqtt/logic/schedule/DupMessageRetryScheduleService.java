package com.meizu.xjsd.mqtt.logic.schedule;

import cn.hutool.core.thread.ThreadFactoryBuilder;
import com.meizu.xjsd.mqtt.logic.config.MqttLogicConfig;
import com.meizu.xjsd.mqtt.logic.service.store.DupPublishMessageStoreDTO;
import com.meizu.xjsd.mqtt.logic.service.store.IDupPublishMessageStoreService;
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
            Iterator<ITransport> iterator = transportLocalStoreService.list().iterator();
            while (iterator.hasNext()) {
                ITransport transport = iterator.next();

                try {
                    if (!transport.isConnected()) {
                        iterator.remove();
                        continue;
                    }
                    sendDupMessage(transport);
                } catch (Exception e) {
                    log.error("Error during retrying duplicate publish messages for transport: {}", transport.clientIdentifier(), e);
                }
            }


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
        log.debug("Retrying duplicate publish messages for transport: {}, messages: {}", transport.clientIdentifier(), messages);
        if (messages != null && !messages.isEmpty()) {
            messages.forEach(message -> {
                log.debug("Sending duplicate message: {}, topic: {}, qos: {}, messageId: {}",
                        message.getMessageId(), message.getTopic(), message.getMqttQoS(), message.getMessageId());
                transport.publish(message.getTopic(), message.getMessageBytes(),
                        MqttQoS.valueOf(message.getMqttQoS()), true, false, message.getMessageId());
            });
        }
    }
}
