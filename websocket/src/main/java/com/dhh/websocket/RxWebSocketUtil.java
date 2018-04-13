package com.dhh.websocket;

import android.os.SystemClock;
import android.support.v4.util.ArrayMap;
import android.util.Log;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.X509TrustManager;

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
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;


/**
 * Created by dhh on 2017/9/21.
 * <p>
 * WebSocketUtil based on okhttp and RxJava
 * </p>
 * Core Feature : WebSocket will be auto reconnection onFailed.
 * <p>
 * please use {@link RxWebSocket} to instead of it
 * <p>
 */
@Deprecated
public class RxWebSocketUtil {
    private static RxWebSocketUtil instance;

    private OkHttpClient client;

    private Map<String, Observable<WebSocketInfo>> observableMap;
    private Map<String, WebSocket> webSocketMap;
    private boolean showLog;
    private String logTag = "RxWebSocket";
    private long interval = 1;
    private TimeUnit reconnectIntervalTimeUnit = TimeUnit.SECONDS;

    private RxWebSocketUtil() {
        try {
            Class.forName("okhttp3.OkHttpClient");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Must be dependency okhttp3 !");
        }
        try {
            Class.forName("rx.Observable");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Must be dependency rxjava 1.x");
        }
        try {
            Class.forName("rx.android.schedulers.AndroidSchedulers");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Must be dependency rxandroid 1.x");
        }
        observableMap = new ArrayMap<>();
        webSocketMap = new ArrayMap<>();
        client = new OkHttpClient();
    }

    /**
     * please use {@link RxWebSocket} to instead of it
     *
     * @return
     */
    @Deprecated
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
     * wss support
     *
     * @param sslSocketFactory
     * @param trustManager
     */
    public void setSSLSocketFactory(SSLSocketFactory sslSocketFactory, X509TrustManager trustManager) {
        client = client.newBuilder().sslSocketFactory(sslSocketFactory, trustManager).build();
    }

    public void setShowLog(boolean showLog) {
        this.showLog = showLog;
    }

    public void setShowLog(boolean showLog, String logTag) {
        setShowLog(showLog);
        this.logTag = logTag;
    }

    public void setReconnectInterval(long interval, TimeUnit timeUnit) {
        this.interval = interval;
        this.reconnectIntervalTimeUnit = timeUnit;

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
                    //自动重连
                    .timeout(timeout, timeUnit)
                    .retry()
                    //共享
                    .doOnUnsubscribe(new Action0() {
                        @Override
                        public void call() {
                            observableMap.remove(url);
                            webSocketMap.remove(url);
                            if (showLog) {
                                Log.d(logTag, "unsubscribe");
                            }
                        }
                    })
                    .doOnNext(new Action1<WebSocketInfo>() {
                        @Override
                        public void call(WebSocketInfo webSocketInfo) {
                            if (webSocketInfo.isOnOpen()) {
                                webSocketMap.put(url, webSocketInfo.getWebSocket());
                            }
                        }
                    })
                    .share()
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread());
            observableMap.put(url, observable);
        } else {
            WebSocket webSocket = webSocketMap.get(url);
            if (webSocket != null) {
                observable = observable.startWith(new WebSocketInfo(webSocket, true));
            }
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
                })
                .filter(new Func1<String, Boolean>() {
                    @Override
                    public Boolean call(String s) {
                        return s != null;
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
                })
                .filter(new Func1<ByteString, Boolean>() {
                    @Override
                    public Boolean call(ByteString byteString) {
                        return byteString != null;
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

    /**
     * 如果url的WebSocket已经打开,可以直接调用这个发送消息.
     *
     * @param url
     * @param msg
     */
    public void send(String url, String msg) {
        WebSocket webSocket = webSocketMap.get(url);
        if (webSocket != null) {
            webSocket.send(msg);
        } else {
            throw new IllegalStateException("The WebSokcet not open");
        }
    }

    /**
     * 如果url的WebSocket已经打开,可以直接调用这个发送消息.
     *
     * @param url
     * @param byteString
     */
    public void send(String url, ByteString byteString) {
        WebSocket webSocket = webSocketMap.get(url);
        if (webSocket != null) {
            webSocket.send(byteString);
        } else {
            throw new IllegalStateException("The WebSokcet not open");
        }
    }

    /**
     * 不用关心url 的WebSocket是否打开,可以直接发送
     *
     * @param url
     * @param msg
     */
    public void asyncSend(String url, final String msg) {
        getWebSocket(url)
                .first()
                .subscribe(new Action1<WebSocket>() {
                    @Override
                    public void call(WebSocket webSocket) {
                        webSocket.send(msg);
                    }
                });

    }

    /**
     * 不用关心url 的WebSocket是否打开,可以直接发送
     *
     * @param url
     * @param byteString
     */
    public void asyncSend(String url, final ByteString byteString) {
        getWebSocket(url)
                .first()
                .subscribe(new Action1<WebSocket>() {
                    @Override
                    public void call(WebSocket webSocket) {
                        webSocket.send(byteString);
                    }
                });
    }

    private Request getRequest(String url) {
        return new Request.Builder().get().url(url).build();
    }

    private final class WebSocketOnSubscribe implements Observable.OnSubscribe<WebSocketInfo> {
        private String url;

        private WebSocket webSocket;

        public WebSocketOnSubscribe(String url) {
            this.url = url;
        }

        @Override
        public void call(final Subscriber<? super WebSocketInfo> subscriber) {
            if (webSocket != null) {
                //降低重连频率
                if (!"main".equals(Thread.currentThread().getName())) {
                    long ms = reconnectIntervalTimeUnit.toMillis(interval);
                    if (ms == 0) {
                        ms = 1000;
                    }
                    SystemClock.sleep(ms);
                    subscriber.onNext(WebSocketInfo.createReconnect());
                }
            }
            initWebSocket(subscriber);
        }

        private void initWebSocket(final Subscriber<? super WebSocketInfo> subscriber) {
            webSocket = client.newWebSocket(getRequest(url), new WebSocketListener() {
                @Override
                public void onOpen(final WebSocket webSocket, Response response) {
                    if (showLog) {
                        Log.d(logTag, url + " --> onOpen");
                    }
                    webSocketMap.put(url, webSocket);
                    if (!subscriber.isUnsubscribed()) {
                        subscriber.onNext(new WebSocketInfo(webSocket, true));
                    }
                }

                @Override
                public void onMessage(WebSocket webSocket, String text) {
                    if (!subscriber.isUnsubscribed()) {
                        subscriber.onNext(new WebSocketInfo(webSocket, text));
                    }
                }

                @Override
                public void onMessage(WebSocket webSocket, ByteString bytes) {
                    if (!subscriber.isUnsubscribed()) {
                        subscriber.onNext(new WebSocketInfo(webSocket, bytes));
                    }
                }

                @Override
                public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                    if (showLog) {
                        Log.e(logTag, t.toString() + webSocket.request().url().uri().getPath());
                    }
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
                    if (showLog) {
                        Log.d(logTag, url + " --> onClosed:code = " + code + ", reason = " + reason);
                    }
                }
            });
            subscriber.add(new MainThreadSubscription() {
                @Override
                protected void onUnsubscribe() {
                    webSocket.close(3000, "close WebSocket");
                    if (showLog) {
                        Log.d(logTag, url + " --> onUnsubscribe ");
                    }
                }
            });
        }

    }
}
