package com.meizu.xjsd.mqtt.logic;


import com.meizu.xjsd.mqtt.logic.config.MqttLogicConfig;
import com.meizu.xjsd.mqtt.logic.schedule.DupMessageRetryScheduleService;
import com.meizu.xjsd.mqtt.logic.service.auth.IAuthService;
import com.meizu.xjsd.mqtt.logic.service.handler.impl.*;
import com.meizu.xjsd.mqtt.logic.service.internal.CompositePublishService;
import com.meizu.xjsd.mqtt.logic.service.internal.IInternalMessageService;
import com.meizu.xjsd.mqtt.logic.service.store.*;
import com.meizu.xjsd.mqtt.logic.service.transport.IClientStoreService;
import com.meizu.xjsd.mqtt.logic.service.transport.ITransportLocalStoreService;
import lombok.Builder;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MqttLogic {
    private final MqttLogicConfig mqttLogicConfig;

    private final IAuthService authService;
    private final IServerPublishMessageStoreService serverPublishMessageStoreService;

    private final IClientPublishMessageStoreService clientPublishMessageStoreService;
    private final IMessageIdService messageIdService;
    private final IRetainMessageStoreService retainMessageStoreService;
    private final ISubscribeStoreService subscribeStoreService;
    private final ISessionStoreService sessionStoreService;
    private final ITransportLocalStoreService transportLocalStoreService;
    private final IInternalMessageService internalMessageService;

    private final IClientStoreService clientStoreService;

    private final CompositePublishService compositePublishService;

    private static ExecutorService executorService;
    private static ExecutorService connectionService;
    private static ExecutorService publishService;

    @Builder
    public MqttLogic(MqttLogicConfig mqttLogicConfig, IAuthService authService,
                     IServerPublishMessageStoreService serverPublishMessageStoreService,
                     IMessageIdService messageIdService,
                     IRetainMessageStoreService retainMessageStoreService,
                     ISubscribeStoreService subscribeStoreService,
                     ISessionStoreService sessionStoreService,
                     ITransportLocalStoreService transportLocalStoreService,
                     IInternalMessageService internalMessageService,
                     IClientStoreService clientStoreService,
                     IClientPublishMessageStoreService clientPublishMessageStoreService) {
        this.mqttLogicConfig = mqttLogicConfig;
        this.authService = authService;
        this.serverPublishMessageStoreService = serverPublishMessageStoreService;
        this.messageIdService = messageIdService;
        this.retainMessageStoreService = retainMessageStoreService;
        this.subscribeStoreService = subscribeStoreService;
        this.sessionStoreService = sessionStoreService;
        this.transportLocalStoreService = transportLocalStoreService;
        this.internalMessageService = internalMessageService;
        this.clientStoreService = clientStoreService;
        this.clientPublishMessageStoreService = clientPublishMessageStoreService;
        this.compositePublishService = new CompositePublishService(
                clientPublishMessageStoreService,
                serverPublishMessageStoreService,
                subscribeStoreService,
                messageIdService,
                internalMessageService
        );
        dupMessageRetryScheduleService = new DupMessageRetryScheduleService(
                mqttLogicConfig,
                serverPublishMessageStoreService,
                transportLocalStoreService
        );
        executorService = Executors.newVirtualThreadPerTaskExecutor();
        connectionService = Executors.newVirtualThreadPerTaskExecutor();
        publishService = Executors.newCachedThreadPool();
    }


    private DupMessageRetryScheduleService dupMessageRetryScheduleService;

    private MqttConnectHandler mqttConnectHandler;
    private MqttDisConnectHandler mqttDisConnectHandler;
    private MqttPublishAckHandler mqttPublishAckHandler;
    private MqttPublishCompHandler mqttPublishCompHandler;
    private MqttPublishMessageHandler mqttPublishMessageHandler;

    private MqttPubrecHandler mqttPubrecHandler;

    private MqttPubrelHandler mqttPubrelHandler;

    private MqttSubscribeMessageHandler mqttSubscribeHandler;
    private MqttUnSubscribeMessageHandler mqttUnSubscribeHandler;

    public boolean isSessionPresent(String clientId) {
        return sessionStoreService.containsKey(clientId);
    }

    public static ExecutorService getExecutorService() {
        return executorService;
    }

    public static ExecutorService getConnectionService() {
        return connectionService;
    }

    public static ExecutorService getPublishService() {
        return publishService;
    }


    public MqttLogicConfig getMqttLogicConfig() {
        return mqttLogicConfig;
    }

    public MqttUnSubscribeMessageHandler unSubscribe() {
        if (mqttUnSubscribeHandler == null) {
            mqttUnSubscribeHandler = new MqttUnSubscribeMessageHandler(
                    subscribeStoreService
            );
        }
        return mqttUnSubscribeHandler;
    }

    public MqttSubscribeMessageHandler subscribe() {
        if (mqttSubscribeHandler == null) {
            mqttSubscribeHandler = new MqttSubscribeMessageHandler(
                    subscribeStoreService,
                    retainMessageStoreService,
                    messageIdService
            );
        }
        return mqttSubscribeHandler;
    }

    public MqttPublishCompHandler publishComp() {
        if (mqttPublishCompHandler == null) {
            mqttPublishCompHandler = new MqttPublishCompHandler(
                    serverPublishMessageStoreService
            );
        }
        return mqttPublishCompHandler;
    }

    public MqttPubrecHandler pubrec() {
        if (mqttPubrecHandler == null) {
            mqttPubrecHandler = new MqttPubrecHandler(
            );
        }
        return mqttPubrecHandler;
    }

    public MqttPubrelHandler pubrel() {
        if (mqttPubrelHandler == null) {
            mqttPubrelHandler = new MqttPubrelHandler(
                    compositePublishService
            );
        }
        return mqttPubrelHandler;
    }

    public MqttPublishAckHandler publishAck() {
        if (mqttPublishAckHandler == null) {
            mqttPublishAckHandler = new MqttPublishAckHandler(
                    serverPublishMessageStoreService
            );
        }
        return mqttPublishAckHandler;
    }

    public MqttPublishMessageHandler publish() {
        if (mqttPublishMessageHandler == null) {
            mqttPublishMessageHandler = new MqttPublishMessageHandler(
                    sessionStoreService,
                    retainMessageStoreService,
                    compositePublishService
            );
        }
        return mqttPublishMessageHandler;
    }

    public MqttConnectHandler connect() {
        if (mqttConnectHandler == null) {
            mqttConnectHandler = new MqttConnectHandler(
                    authService,
                    transportLocalStoreService,
                    sessionStoreService,
                    serverPublishMessageStoreService,
                    clientStoreService,
                    mqttLogicConfig.getBrokerId()
            );
        }
        return mqttConnectHandler;
    }

    public MqttDisConnectHandler disConnect() {
        if (mqttDisConnectHandler == null) {
            mqttDisConnectHandler = new MqttDisConnectHandler(
                    transportLocalStoreService,
                    sessionStoreService,
                    subscribeStoreService,
                    clientStoreService,
                    compositePublishService
            );
        }
        return mqttDisConnectHandler;
    }


}
