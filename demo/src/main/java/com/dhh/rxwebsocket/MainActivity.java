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
import com.jakewharton.rxbinding.view.RxView;

import okhttp3.WebSocket;
import okio.ByteString;
import rx.Observable;
import rx.Subscription;
import rx.functions.Action1;
import rx.functions.Func1;
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
        //from : http://www.blue-zero.com/WebSocket/     .  thanks
        url = "ws://121.40.165.18:8088";

        //if you want to set your okhttpClient
//        OkHttpClient yourClient = new OkHttpClient();
//        RxWebSocketUtil.getInstance().setClient(yourClient);

        final int type = 2;  // 1, 2, 3.
        contect(type);


        //send message
        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mWebSocket != null) {
                    mWebSocket.send(editText.getText().toString());
                } else {
                    RxWebSocketUtil.getInstance().send(url, editText.getText().toString());
                }
            }
        });
    }

    //=================================== my lifecycle ===================================//
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
//=================================== my lifecycle ===================================//


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
