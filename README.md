## open-mqtt
open-mqtt是一款基于Java开发的轻量级高并发MQTT Broker，采用Netty和vertx实现异步通信，完整支持MQTT 5.0协议，包括QoS消息分级、主题通配符、消息持久化等核心功能。开发者可自定义认证逻辑和存储方案

### 集群
采用ignite 实现集群功能，支持水平扩展和高可用部署
### 存储
支持 casandra, ignite 等多种存储方案
