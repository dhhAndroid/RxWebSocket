# RxWebSocket #
## RxWebSocket是一个基于okhttp和RxJava封装的WebSocket客户端,此库的核心特点是  除了手动关闭WebSocket(就是RxJava取消订阅),WebSocket在异常关闭的时候(onFailure,发生异常,如WebSocketException等等),会自动重连,永不断连.其次,对WebSocket做的缓存处理,同一个URL,共享一个WebSocket.
## 效果图 ##
![](image/WebSocket.gif)

## Usage ##
### open WebSocket

```
RxWebSocketUtil.getInstance().getWebSocketInfo(url)
                        .subscribe(new Action1<WebSocketInfo>() {
                            @Override
                            public void call(WebSocketInfo webSocketInfo) {
                                mWebSocket = webSocketInfo.getWebSocket();
                                Log.d("MainActivity", webSocketInfo.getString());
                                Log.d("MainActivity", "webSocketInfo.getByteString():" + webSocketInfo.getByteString());
                            }
                        });

mWebSocket.send("hello word");
```
```  
// Rxbinding
                RxView.clicks(centect)
                        .flatMap(new Func1<Void, Observable<String>>() {
                            @Override
                            public Observable<String> call(Void aVoid) {
                                return RxWebSocketUtil.getInstance().getWebSocketString(url);
                            }
                        })
                        .subscribe(new Action1<String>() {
                            @Override
                            public void call(String s) {
                                //the s !=null

                                Log.d("MainActivity", s);
                                textview.setText(Html.fromHtml(s));

                            }
                        });
```
### 发送消息 ###
```
mWebSocket.send("hello word");

//如果指定URL的WebSocket已经打开,也可以这样使用
RxWebSocketUtil.getInstance().send(url, "hello word");
```
### 注销 ###
 RxJava的注销方式,就可以取消订阅. 项目里的demo里,简单实现了一个RxLifecycle.仅供参考.

License
-------

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.