package com.dhh.rxwebsocket;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.dhh.rxlifecycle.RxLifecycle;
import com.dhh.websocket.Config;
import com.dhh.websocket.RxWebSocket;
import com.dhh.websocket.WebSocketInfo;
import com.dhh.websocket.WebSocketSubscriber;

import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okio.ByteString;
import rx.Subscription;
import rx.functions.Action0;
import rx.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity {
    private WebSocket mWebSocket;
    private EditText editText;
    private Button send;
    private Button centect;
    private Subscription mSubscription;
    private TextView textview;
    private String url;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();


        //init config
        Config config = new Config.Builder()
                .setShowLog(true)           //show  log
//                .setClient(yourClient)   //if you want to set your okhttpClient
//                .setShowLog(true, "your logTag")
//                .setReconnectInterval(2, TimeUnit.SECONDS)  //set reconnect interval
//                .setSSLSocketFactory(yourSSlSocketFactory, yourX509TrustManager) // wss support
                .build();

        RxWebSocket.setConfig(config);
        // please use WebSocketSubscriber
        RxWebSocket.get("your url")
                //RxLifecycle : https://github.com/dhhAndroid/RxLifecycle
                .compose(RxLifecycle.with(this).<WebSocketInfo>bindOnDestroy())
                .subscribe(new WebSocketSubscriber() {
                    @Override
                    public void onOpen(@NonNull WebSocket webSocket) {
                        Log.d("MainActivity", "onOpen1:");
                    }

                    @Override
                    public void onMessage(@NonNull String text) {
                        Log.d("MainActivity", "返回数据:" + text);
                    }

                    @Override
                    public void onMessage(@NonNull ByteString byteString) {

                    }

                    @Override
                    protected void onReconnect() {
                        Log.d("MainActivity", "重连:");
                    }
                });
        Subscription subscription = RxWebSocket.get("ws://sdfsd")
                .subscribe(new WebSocketSubscriber() {
                    @Override
                    protected void onClose() {
                        Log.d("MainActivity", "直接关闭");
                    }
                });
        if (subscription != null && !subscription.isUnsubscribed()) {
            subscription.unsubscribe();
        }
        initDemo();
    }

    @Override
    protected void onResume() {
        super.onResume();

        RxWebSocket.get("url")
                .subscribe(new WebSocketSubscriber() {
                    @Override
                    protected void onMessage(@NonNull String text) {

                    }
                });

        RxWebSocket.get("your url")
                //RxLifecycle : https://github.com/dhhAndroid/RxLifecycle
                .compose(RxLifecycle.with(this).<WebSocketInfo>bindToLifecycle())
                .subscribe(new WebSocketSubscriber() {
                    @Override
                    public void onOpen(@NonNull WebSocket webSocket) {
                        Log.d("MainActivity", "onOpen1:");
                    }

                    @Override
                    public void onMessage(@NonNull String text) {
                        Log.d("MainActivity", "返回数据:" + text);
                    }

                    @Override
                    public void onMessage(@NonNull ByteString byteString) {

                    }

                    @Override
                    protected void onReconnect() {
                        Log.d("MainActivity", "重连:");
                    }

                    @Override
                    protected void onClose() {
                        Log.d("MainActivity", "onClose:");
                    }
                });
    }

    private void initDemo() {
        Schedulers.io().createWorker().schedule(new Action0() {
            @Override
            public void call() {
                initServerWebsocket();
            }
        });
        setListener();

        // unsubscribe
        Subscription subscription = RxWebSocket.get("ws://sdfs").subscribe();
        if (subscription != null && !subscription.isUnsubscribed()) {
            subscription.unsubscribe();
        }
    }

    private void initServerWebsocket() {
        final MockWebServer mockWebServer = new MockWebServer();
        url = "ws://" + mockWebServer.getHostName() + ":" + mockWebServer.getPort() + "/";
        mockWebServer.enqueue(new MockResponse().withWebSocketUpgrade(new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                webSocket.send("hello, I am  dhhAndroid !");
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                Log.d("MainActivity", "收到客户端消息:" + text);
                webSocket.send("Server response:" + text);
            }

            @Override
            public void onClosed(WebSocket webSocket, int code, String reason) {
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, Response response) {
            }
        }));
    }


    private void setListener() {
        //send message
        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String msg = editText.getText().toString();
                if (mWebSocket != null) {
                    mWebSocket.send(msg);
                } else {
                    send();
                }
            }
        });
        centect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //注意取消订阅,有多种方式,比如 rxlifecycle
                connect();

            }
        });

    }

    private void connect() {
        RxWebSocket.get(url)
                //RxLifecycle : https://github.com/dhhAndroid/RxLifecycle
                .compose(RxLifecycle.with(this).<WebSocketInfo>bindOnDestroy())
                .subscribe(new WebSocketSubscriber() {
                    @Override
                    protected void onOpen(@NonNull WebSocket webSocket) {
                        Log.d("MainActivity", " on WebSocket open");
                    }

                    @Override
                    protected void onMessage(@NonNull String text) {
                        Log.d("MainActivity", text);
                        textview.setText(Html.fromHtml(text));
                    }

                    @Override
                    protected void onMessage(@NonNull ByteString byteString) {
                        Log.d("MainActivity", byteString.toString());
                    }

                    @Override
                    protected void onReconnect() {
                        Log.d("MainActivity", "onReconnect");
                    }

                    @Override
                    protected void onClose() {
                        Log.d("MainActivity", "onClose");
                    }

                    @Override
                    public void onError(Throwable e) {
                        super.onError(e);
                    }
                });
    }

    public void send() {
        //url 对应的WebSocket 必须打开,否则报错
        RxWebSocket.send(url, "hello");
        RxWebSocket.send(url, ByteString.EMPTY);
        //异步发送,若WebSocket已经打开,直接发送,若没有打开,打开一个WebSocket发送完数据,直接关闭.
        RxWebSocket.asyncSend(url, "hello");
        RxWebSocket.asyncSend(url, ByteString.EMPTY);
    }

    private void initView() {
        editText = (EditText) findViewById(R.id.editText);
        send = (Button) findViewById(R.id.send);
        centect = (Button) findViewById(R.id.centect);
        textview = (TextView) findViewById(R.id.textview);
    }

}
