package com.dhh.rxwebsocket;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.dhh.rxlifecycle2.RxLifecycle;
import com.dhh.websocket.Config;
import com.dhh.websocket.RxWebSocket;
import com.dhh.websocket.WebSocketInfo;
import com.dhh.websocket.WebSocketSubscriber;

import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okio.ByteString;

public class MainActivity extends AppCompatActivity {
    private WebSocket mWebSocket;
    private EditText editText;
    private Button send;
    private Button centect;
    private Disposable mDisposable;
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

        // use WebSocketSubscriber
        RxWebSocket.get("ws://10.7.5.88:8089/status")
                //RxLifecycle : https://github.com/dhhAndroid/RxLifecycle
                .compose(RxLifecycle.with(this).<WebSocketInfo>bindToLifecycle())
                .subscribe(new WebSocketSubscriber() {
                    @Override
                    public void onOpen(@NonNull WebSocket webSocket) {
                        Log.d("MainActivity", "onOpen1:");
                    }

                    @Override
                    public void onMessage(@NonNull String text) {
                        Log.d("MainActivity", text);
                    }

                    @Override
                    public void onMessage(@NonNull ByteString bytes) {

                    }

                    @Override
                    protected void onReconnect() {
                        Log.d("MainActivity", "重连");
                    }

                    @Override
                    protected void onClose() {

                    }

                    @Override
                    public void onError(Throwable e) {

                    }
                });


        RxWebSocket.get("ws://10.7.5.88:8089/status")
                .subscribe(new WebSocketSubscriber() {
                    @Override
                    protected void onMessage(String text) {

                    }

                    @Override
                    protected void onReconnect() {
                        Log.d("MainActivity", "重连");
                    }
                });

        //注销
        Disposable disposable = RxWebSocket.get("ws://sdfs").subscribe();
        if (disposable != null && !disposable.isDisposed()) {
            disposable.dispose();
        }

        initDemo();
    }

    @Override
    protected void onResume() {
        super.onResume();

        RxWebSocket.get("url")
                .compose(RxLifecycle.with(this).<WebSocketInfo>bindToLifecycle())
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
        Schedulers.io().createWorker().schedule(new Runnable() {
            @Override
            public void run() {
                initServerWebsocket();
            }
        });

        setListener();
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
                connect();

            }
        });

    }

    private void connect() {
        if (mDisposable != null) return;
        //注意取消订阅,有多种方式,比如 rxlifecycle
        mDisposable = RxWebSocket.get(url)
                // RxLifeCycle: https://github.com/dhhAndroid/RxLifecycle
                .compose(RxLifecycle.with(this).<WebSocketInfo>bindOnDestroy())
                .subscribe(new Consumer<WebSocketInfo>() {
                    @Override
                    public void accept(WebSocketInfo webSocketInfo) throws Exception {
                        mWebSocket = webSocketInfo.getWebSocket();
                        if (webSocketInfo.isOnOpen()) {
                            Log.d("MainActivity", " on WebSocket open");
                        } else {

                            String string = webSocketInfo.getString();
                            if (string != null) {
                                Log.d("MainActivity", string);
                                textview.setText(Html.fromHtml(string));

                            }

                            ByteString byteString = webSocketInfo.getByteString();
                            if (byteString != null) {
                                Log.d("MainActivity", "webSocketInfo.getByteString():" + byteString);

                            }
                        }

                    }
                });
    }


    public void send() {
        //引用直接发
        mWebSocket.send("hello");
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mDisposable != null) {
            mDisposable.dispose();
        }
    }
}
