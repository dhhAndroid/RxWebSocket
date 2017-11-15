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
import com.dhh.websocket.RxWebSocketUtil;
import com.dhh.websocket.WebSocketInfo;
import com.dhh.websocket.WebSocketSubscriber;
import com.dhh.websocket.WebSokcetAction1;
import com.jakewharton.rxbinding.view.RxView;

import java.util.concurrent.TimeUnit;

import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okio.ByteString;
import rx.Observable;
import rx.Subscription;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;
import rx.subjects.BehaviorSubject;

public class MainActivity extends AppCompatActivity {
    private BehaviorSubject<ActivityEvent> lifeCycle = BehaviorSubject.create();
    private WebSocket mWebSocket;
    private EditText editText;
    private Button send;
    private Button centect;
    private Subscription mSubscription;
    private TextView textview;
    private String url;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        lifeCycle.onNext(ActivityEvent.onCreate);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();


        Schedulers.io().createWorker().schedule(new Action0() {
            @Override
            public void call() {
                initServerWebsocket();
            }
        });

        //if you want to set your okhttpClient
//        OkHttpClient yourClient = new OkHttpClient();
//        RxWebSocketUtil.getInstance().setClient(yourClient);

        //wss support
//        RxWebSocketUtil.getInstance().setSSLSocketFactory(yourSSlSocketFactory,yourX509TrustManager);
//        RxWebSocketUtil.getInstance().getWebSocket("wss://...");
        //or
//        OkHttpClient client = new OkHttpClient.Builder()
//                .sslSocketFactory(yourSSlSocketFactory, yourX509TrustManager)
//                other config...
//                .build();
//        RxWebSocketUtil.getInstance().setClient(client);
        RxWebSocketUtil.getInstance().setShowLog(BuildConfig.DEBUG);
        final int type = 2;  // 1, 2, 3.
        contect(type);
        // use WebSocketSubscriber
        RxWebSocketUtil.getInstance().getWebSocketInfo("ws://10.7.5.88:8089")
                //RxLifecycle : https://github.com/dhhAndroid/RxLifecycle
                .compose(RxLifecycle.with(this).<WebSocketInfo>bindOnDestroy())
                .subscribe(new WebSocketSubscriber() {
                    @Override
                    public void onOpen(@NonNull WebSocket webSocket) {
                        Log.d("MainActivity", "onOpen1:");
                    }

                    @Override
                    public void onMessage(@NonNull String text) {
                    }

                    @Override
                    public void onMessage(@NonNull ByteString bytes) {

                    }
                });
        // use WebSokcetAction1
        RxWebSocketUtil.getInstance().getWebSocketInfo("ws://10.7.5.88:8089")
                //RxLifecycle : https://github.com/dhhAndroid/RxLifecycle
                .compose(RxLifecycle.with(this).<WebSocketInfo>bindOnDestroy())
                .subscribe(new WebSokcetAction1() {
                    @Override
                    public void onOpen(@NonNull WebSocket webSocket) {
                        Log.d("MainActivity", "onOpen2:");
                    }

                    @Override
                    public void onMessage(@NonNull String text) {

                    }

                    @Override
                    public void onMessage(@NonNull ByteString bytes) {

                    }
                });

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

        Subscription subscription = RxWebSocketUtil.getInstance().getWebSocketString("ws://sdfs").subscribe();
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

    //=================================== my lifecycle  start===================================//
    public Observable<ActivityEvent> bindOndestroy() {
        return lifeCycle.takeFirst(new Func1<ActivityEvent, Boolean>() {
            @Override
            public Boolean call(ActivityEvent activityEvent) {
                return activityEvent == ActivityEvent.onDestory;
            }
        });
    }

    public <T> Lifecycle<T> bindOnActivityEvent(ActivityEvent event) {
        return new Lifecycle<>(event);
    }


    public class Lifecycle<T> implements Observable.Transformer<T, T> {
        private ActivityEvent mActivityEvent;

        public Lifecycle() {
        }

        public Lifecycle(ActivityEvent activityEvent) {
            mActivityEvent = activityEvent;
        }

        @Override
        public Observable<T> call(Observable<T> tObservable) {
            if (mActivityEvent == null) {
                return tObservable.takeUntil(bindOndestroy());

            } else {
                return tObservable.takeUntil(lifeCycle.takeFirst(new Func1<ActivityEvent, Boolean>() {
                    @Override
                    public Boolean call(ActivityEvent event) {
                        return event == mActivityEvent;
                    }
                }));
            }
        }
    }
//=================================== my lifecycle  end===================================//


    private void contect(int type) {
        switch (type) {
            case 1:
                centect.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (mSubscription != null) return;
                        //注意取消订阅,有多种方式,比如 rxlifecycle
                        mSubscription = RxWebSocketUtil.getInstance().getWebSocketInfo(url)
                                //bind on life
                                .takeUntil(bindOndestroy())
                                .subscribe(new Action1<WebSocketInfo>() {
                                    @Override
                                    public void call(WebSocketInfo webSocketInfo) {
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
                });
                break;
            case 2:
                //RxBinding
                RxView.clicks(centect)
                        .flatMap(new Func1<Void, Observable<WebSocketInfo>>() {
                            @Override
                            public Observable<WebSocketInfo> call(Void aVoid) {
                                return RxWebSocketUtil.getInstance().getWebSocketInfo(url);
                            }
                        })
                        //bind on life
                        .compose(new Lifecycle<WebSocketInfo>())
                        .subscribe(new Action1<WebSocketInfo>() {
                            @Override
                            public void call(WebSocketInfo webSocketInfo) {
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
                break;
            case 3:
                RxView.clicks(centect)
                        .flatMap(new Func1<Void, Observable<String>>() {
                            @Override
                            public Observable<String> call(Void aVoid) {
                                return RxWebSocketUtil.getInstance().getWebSocketString(url);
                            }
                        })
                        //bind on life
                        .compose(this.<String>bindOnActivityEvent(ActivityEvent.onDestory))
                        .subscribe(new Action1<String>() {
                            @Override
                            public void call(String s) {
                                //the s !=null

                                Log.d("MainActivity", s);
                                textview.setText(Html.fromHtml(s));

                            }
                        });
                break;
        }

    }


    public void otherUse() {
        //get StringMsg
        RxWebSocketUtil.getInstance().getWebSocketString(url)
                .subscribe(new Action1<String>() {
                    @Override
                    public void call(String s) {
                    }
                });
        // get ByteString
        RxWebSocketUtil.getInstance().getWebSocketByteString(url)
                .subscribe(new Action1<ByteString>() {
                    @Override
                    public void call(ByteString byteString) {

                    }
                });
        //get WebSocket
        RxWebSocketUtil.getInstance().getWebSocket(url)
                .subscribe(new Action1<WebSocket>() {
                    @Override
                    public void call(WebSocket webSocket) {

                    }
                });
        //with timeout
        RxWebSocketUtil.getInstance().getWebSocketInfo(url, 10, TimeUnit.SECONDS)
                .subscribe(new Action1<WebSocketInfo>() {
                    @Override
                    public void call(WebSocketInfo webSocketInfo) {

                    }
                });
    }

    public void send() {
        //url 对应的WebSocket 必须打开,否则报错
        RxWebSocketUtil.getInstance().send(url, "hello");
        RxWebSocketUtil.getInstance().send(url, ByteString.EMPTY);
        //异步发送,若WebSocket已经打开,直接发送,若没有打开,打开一个WebSocket发送完数据,直接关闭.
        RxWebSocketUtil.getInstance().asyncSend(url, "hello");
        RxWebSocketUtil.getInstance().asyncSend(url, ByteString.EMPTY);
    }

    private void initView() {
        editText = (EditText) findViewById(R.id.editText);
        send = (Button) findViewById(R.id.send);
        centect = (Button) findViewById(R.id.centect);
        textview = (TextView) findViewById(R.id.textview);
    }

    @Override
    protected void onDestroy() {
        lifeCycle.onNext(ActivityEvent.onDestory);
        super.onDestroy();
        if (mSubscription != null) {
            mSubscription.unsubscribe();
        }
    }
}
