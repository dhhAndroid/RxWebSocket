package com.dhh.rxwebsocket;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.dhh.websocket.RxWebSocketUtil;
import com.dhh.websocket.WebSocketInfo;
import com.jakewharton.rxbinding2.view.RxView;

import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.ObservableTransformer;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.BehaviorSubject;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okio.ByteString;

public class MainActivity extends AppCompatActivity {
    private BehaviorSubject<ActivityEvent> lifeCycle = BehaviorSubject.create();
    private WebSocket mWebSocket;
    private EditText editText;
    private Button send;
    private Button centect;
    private Disposable mDisposable;
    private TextView textview;
    private String url;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        lifeCycle.onNext(ActivityEvent.onCreate);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();


        Schedulers.io().createWorker().schedule(new Runnable() {
            @Override
            public void run() {
                initServerWebsocket();
            }
        });

        //if you want to set your okhttpClient
//        OkHttpClient yourClient = new OkHttpClient();
//        RxWebSocketUtil.getInstance().setClient(yourClient);

        RxWebSocketUtil.getInstance().setShowLog(BuildConfig.DEBUG);
        final int type = 2;  // 1, 2, 3.
        contect(type);


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

        Disposable disposable = RxWebSocketUtil.getInstance().getWebSocketString("ws://sdfs").subscribe();
        if (disposable != null && !disposable.isDisposed()) {
            disposable.dispose();
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
        return lifeCycle.filter(new Predicate<ActivityEvent>() {
            @Override
            public boolean test(@NonNull ActivityEvent activityEvent) throws Exception {
                return activityEvent == ActivityEvent.onDestory;
            }
        });
    }

    public <T> Lifecycle<T> bindOnActivityEvent(ActivityEvent event) {
        return new Lifecycle<>(event);
    }


    public class Lifecycle<T> implements ObservableTransformer<T, T> {
        private ActivityEvent mActivityEvent;

        public Lifecycle() {
        }

        public Lifecycle(ActivityEvent activityEvent) {
            mActivityEvent = activityEvent;
        }

        @Override
        public ObservableSource<T> apply(@NonNull Observable<T> upstream) {
            if (mActivityEvent == null) {
                return upstream.takeUntil(bindOndestroy());

            } else {
                return upstream.takeUntil(lifeCycle.filter(new Predicate<ActivityEvent>() {
                    @Override
                    public boolean test(@NonNull ActivityEvent activityEvent) throws Exception {
                        return activityEvent == ActivityEvent.onDestory;
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
                        if (mDisposable != null) return;
                        //注意取消订阅,有多种方式,比如 rxlifecycle
                        mDisposable = RxWebSocketUtil.getInstance().getWebSocketInfo(url)
                                //bind on life
                                .takeUntil(bindOndestroy())
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
                });
                break;
            case 2:
                //RxBinding
                RxView.clicks(centect)
                        .flatMap(new Function<Object, ObservableSource<WebSocketInfo>>() {
                            @Override
                            public ObservableSource<WebSocketInfo> apply(@NonNull Object o) throws Exception {
                                return RxWebSocketUtil.getInstance().getWebSocketInfo(url);
                            }
                        })
                        .compose(new Lifecycle<WebSocketInfo>())
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
                break;
            case 3:
                RxView.clicks(centect)
                        .flatMap(new Function<Object, ObservableSource<String>>() {
                            @Override
                            public ObservableSource<String> apply(@NonNull Object o) throws Exception {
                                return RxWebSocketUtil.getInstance().getWebSocketString(url);
                            }
                        })
                        //bind on life
                        .compose(this.<String>bindOnActivityEvent(ActivityEvent.onDestory))
                        .subscribe(new Consumer<String>() {
                            @Override
                            public void accept(String s) throws Exception {
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
                .subscribe(new Consumer<String>() {
                    @Override
                    public void accept(String s) throws Exception {

                    }
                });
        // get ByteString
        RxWebSocketUtil.getInstance().getWebSocketByteString(url)
                .subscribe(new Consumer<ByteString>() {
                    @Override
                    public void accept(ByteString byteString) throws Exception {

                    }
                });
        //get WebSocket
        RxWebSocketUtil.getInstance().getWebSocket(url)
                .subscribe(new Consumer<WebSocket>() {
                    @Override
                    public void accept(WebSocket webSocket) throws Exception {

                    }
                });
        //with timeout
        RxWebSocketUtil.getInstance().getWebSocketInfo(url, 10, TimeUnit.SECONDS)
                .subscribe(new Consumer<WebSocketInfo>() {
                    @Override
                    public void accept(WebSocketInfo webSocketInfo) throws Exception {

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
        if (mDisposable != null) {
            mDisposable.dispose();
        }
    }
}
