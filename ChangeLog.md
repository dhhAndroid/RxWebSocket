### 2.0.0
 - 增加了config属性，可以配置重连频率，是否打印日志及日志tag等等
 - 重构了调用API，使用更加简洁
 - 对外隐藏了RxWebSocketUtil，现在统一由RxWebSocket代理完成
 - 增加了重连(onReconnect)、关闭(onClose)事件回调
### 1.5.0
 - fix: [#11](https://github.com/dhhAndroid/RxWebSocket/issues/11)
 - 修复WebSocket打开事件发送错误.
 - 简化依赖关系
### 1.4.0
 - fix: [#3](https://github.com/dhhAndroid/RxWebSocket/issues/3),[#4](https://github.com/dhhAndroid/RxWebSocket/issues/4),[#6](https://github.com/dhhAndroid/RxWebSocket/issues/6)
 - 移除WebSocketInfo复用导致数据丢失
 - RxJava1版本 加入WebSocketSubscriber,WebSocketAction1
 - RxJava2版本加入WebSocketSubscriber,WebSocketSonsumer
 - 加入设置SSLSocketFactory快捷方法


### 1.3.0
- 增加RxJava2支持