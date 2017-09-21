package com.dhh.websocket;

import android.os.SystemClock;
import android.support.v4.util.ArrayMap;
import android.util.Log;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;
import rx.Observable;
import rx.Subscriber;
import rx.android.MainThreadSubscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.functions.Func1;
import rx.schedulers.Schedulers;


/**
 * Created by dhh on 2017/9/21.
 * <p>
 * WebSocketUtil based on okhttp and RxJava
 * </p>
 *  Code Feature : WebSocket will be auto reconnection onFailed.
 */
public class RxWebSocketUtil {
    private static RxWebSocketUtil instance;

    private OkHttpClient client;

    private Map<String, Observable<WebSocketInfo>> observableMap;

    private RxWebSocketUtil() {
        try {
            Class.forName("okhttp3.OkHttpClient");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Must be dependency okhtt");
        }
        try {
            Class.forName("rx.Observable");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Must be dependency rxjava 1.+");
        }
        try {
            Class.forName("rx.android.schedulers.AndroidSchedulers");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Must be dependency rxandroid 1.+");
        }
        observableMap = new ArrayMap<>();
        client = new OkHttpClient();
    }

    public static RxWebSocketUtil getInstance() {
        if (instance == null) {
            synchronized (RxWebSocketUtil.class) {
                if (instance == null) {
                    instance = new RxWebSocketUtil();
                }
            }
        }
        return instance;
    }

    /**
     * set your client
     *
     * @param client
     */
    public void setClient(OkHttpClient client) {
        if (client == null) {
            throw new NullPointerException(" Are you stupid ? client == null");
        }
        this.client = client;
    }

    /**
     * @param url      ws://127.0.0.1:8080/websocket
     * @param timeout  The WebSocket will be reconnected after the specified time interval is not "onMessage",
     *                 <p>
     *                 在指定时间间隔后没有收到消息就会重连WebSocket,为了适配小米平板,因为小米平板断网后,不会发送错误通知
     * @param timeUnit unit
     * @return
     */
    public Observable<WebSocketInfo> getWebSocketInfo(final String url, final long timeout, final TimeUnit timeUnit) {
        Observable<WebSocketInfo> observable = observableMap.get(url);
        if (observable == null) {
            observable = Observable.create(new WebSocketOnSubscribe(url))
                    .timeout(timeout, timeUnit)
                    .retry()
                    .doOnUnsubscribe(new Action0() {
                        @Override
                        public void call() {
                            observableMap.remove(url);
                        }
                    })
                    .share()
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread());
            observableMap.put(url, observable);
        }
        return observable;
    }

    /**
     * default timeout: 30 days
     * <p>
     * 若忽略小米平板,请调用这个方法
     * </p>
     */
    public Observable<WebSocketInfo> getWebSocketInfo(String url) {
        return getWebSocketInfo(url, 30, TimeUnit.DAYS);
    }

    public Observable<String> getWebSocketString(String url) {
        return getWebSocketInfo(url)
                .map(new Func1<WebSocketInfo, String>() {
                    @Override
                    public String call(WebSocketInfo webSocketInfo) {
                        return webSocketInfo.getString();
                    }
                });
    }

    public Observable<ByteString> getWebSocketByteString(String url) {
        return getWebSocketInfo(url)
                .map(new Func1<WebSocketInfo, ByteString>() {
                    @Override
                    public ByteString call(WebSocketInfo webSocketInfo) {
                        return webSocketInfo.getByteString();
                    }
                });
    }

    public Observable<WebSocket> getWebSocket(String url) {
        return getWebSocketInfo(url)
                .map(new Func1<WebSocketInfo, WebSocket>() {
                    @Override
                    public WebSocket call(WebSocketInfo webSocketInfo) {
                        return webSocketInfo.getWebSocket();
                    }
                });
    }

    private Request getRequest(String url) {
        return new Request.Builder().get().url(url).build();
    }

    private final class WebSocketOnSubscribe implements Observable.OnSubscribe<WebSocketInfo> {
        private String url;

        private WebSocket webSocket;

        private WebSocketInfo stringInfo, byteStringInfo;

        public WebSocketOnSubscribe(String url) {
            this.url = url;
            stringInfo = new WebSocketInfo();
            byteStringInfo = new WebSocketInfo();
        }

        @Override
        public void call(final Subscriber<? super WebSocketInfo> subscriber) {
            if (webSocket != null) {
                //降低重连频率
                if (!"main".equals(Thread.currentThread().getName())) {
                    SystemClock.sleep(2000);
                }
            }
            webSocket = client.newWebSocket(getRequest(url), new WebSocketListener() {
                @Override
                public void onOpen(final WebSocket webSocket, Response response) {
                    Log.d("WebSocketUtil", url + " --> onOpen");
                    AndroidSchedulers.mainThread().createWorker().schedule(new Action0() {
                        @Override
                        public void call() {
                            if (!subscriber.isUnsubscribed()) {
                                subscriber.onStart();
                            }
                        }
                    });
                }

                @Override
                public void onMessage(WebSocket webSocket, String text) {
                    if (!subscriber.isUnsubscribed()) {
                        stringInfo.setWebSocket(webSocket);
                        stringInfo.setString(text);
                        subscriber.onNext(stringInfo);
                    }
                }

                @Override
                public void onMessage(WebSocket webSocket, ByteString bytes) {
                    if (!subscriber.isUnsubscribed()) {
                        byteStringInfo.setWebSocket(webSocket);
                        byteStringInfo.setByteString(bytes);
                        subscriber.onNext(byteStringInfo);
                    }
                }

                @Override
                public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                    Log.e("WebSocketUtil", t.toString() + webSocket.request().url().uri().getPath());
                    if (!subscriber.isUnsubscribed()) {
                        subscriber.onError(t);
                    }
                }

                @Override
                public void onClosing(WebSocket webSocket, int code, String reason) {
                    webSocket.close(1000, null);
                }

                @Override
                public void onClosed(WebSocket webSocket, int code, String reason) {
                    Log.d("WebSocketUtil", url + " --> onClosed:code= " + code);
                }
            });
            subscriber.add(new MainThreadSubscription() {
                @Override
                protected void onUnsubscribe() {
                    webSocket.close(3000, "手动关闭");
                }
            });
        }

    }
}
