package com.zzy.mqtt.logic;


import com.zzy.mqtt.logic.config.MqttLogicConfig;
import com.zzy.mqtt.logic.schedule.DupMessageRetryScheduleService;
import com.zzy.mqtt.logic.service.auth.IAuthService;
import com.zzy.mqtt.logic.service.handler.impl.*;
import com.zzy.mqtt.logic.service.internal.CompositeStoreService;
import com.zzy.mqtt.logic.service.internal.IInternalMessageService;
import com.zzy.mqtt.logic.service.store.*;
import com.zzy.mqtt.logic.service.transport.IClientStoreService;
import com.zzy.mqtt.logic.service.transport.ITransportLocalStoreService;
import lombok.Builder;

import java.util.concurrent.*;

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

    private final CompositeStoreService compositeStoreService;

//    private final IDistributeLock distributeLock;

    private static ExecutorService protocolService;

    private static ExecutorService connectService;

    private static ExecutorService storeService;


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
        this.compositeStoreService = new CompositeStoreService(
                clientPublishMessageStoreService,
                serverPublishMessageStoreService,
                subscribeStoreService,
                messageIdService,
                sessionStoreService,
                clientStoreService,
                internalMessageService,
                retainMessageStoreService,
                mqttLogicConfig
        );
        dupMessageRetryScheduleService = new DupMessageRetryScheduleService(
                mqttLogicConfig,
                serverPublishMessageStoreService,
                clientPublishMessageStoreService,
                compositeStoreService,
                transportLocalStoreService,
                subscribeStoreService
        );

        connectService = Executors.newVirtualThreadPerTaskExecutor();
        protocolService = Executors.newVirtualThreadPerTaskExecutor();
        storeService = new ThreadPoolExecutor(
                10,
                100,
                20,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(10000),
                new ThreadPoolExecutor.AbortPolicy()
        );

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

    public static ExecutorService getConnectService() {
        return connectService;
    }

    public static ExecutorService getStoreService() {
        return storeService;
    }

    public static ExecutorService getProtocolService() {
        return protocolService;
    }

    public MqttLogicConfig getMqttLogicConfig() {
        return mqttLogicConfig;
    }

    public MqttUnSubscribeMessageHandler unSubscribe() {
        if (mqttUnSubscribeHandler == null) {
            mqttUnSubscribeHandler = new MqttUnSubscribeMessageHandler(
                    compositeStoreService
            );
        }
        return mqttUnSubscribeHandler;
    }

    public MqttSubscribeMessageHandler subscribe() {
        if (mqttSubscribeHandler == null) {
            mqttSubscribeHandler = new MqttSubscribeMessageHandler(
                    compositeStoreService
            );
        }
        return mqttSubscribeHandler;
    }

    public MqttPublishCompHandler publishComp() {
        if (mqttPublishCompHandler == null) {
            mqttPublishCompHandler = new MqttPublishCompHandler(
                    compositeStoreService
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
                    compositeStoreService
            );
        }
        return mqttPubrelHandler;
    }

    public MqttPublishAckHandler publishAck() {
        if (mqttPublishAckHandler == null) {
            mqttPublishAckHandler = new MqttPublishAckHandler(
                    compositeStoreService
            );
        }
        return mqttPublishAckHandler;
    }

    public MqttPublishMessageHandler publish() {
        if (mqttPublishMessageHandler == null) {
            mqttPublishMessageHandler = new MqttPublishMessageHandler(
                    compositeStoreService
            );
        }
        return mqttPublishMessageHandler;
    }

    public MqttConnectHandler connect() {
        if (mqttConnectHandler == null) {
            mqttConnectHandler = new MqttConnectHandler(
                    authService,
                    transportLocalStoreService,
                    compositeStoreService,
                    mqttLogicConfig.getBrokerId()
            );
        }
        return mqttConnectHandler;
    }

    public MqttDisConnectHandler disConnect() {
        if (mqttDisConnectHandler == null) {
            mqttDisConnectHandler = new MqttDisConnectHandler(
                    transportLocalStoreService,
                    compositeStoreService
            );
        }
        return mqttDisConnectHandler;
    }


}
