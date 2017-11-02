package com.dhh.websocket;

import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;

import okhttp3.WebSocket;
import okio.ByteString;
import rx.Subscriber;

/**
 * Created by dhh on 2017/11/2.
 */

public abstract class WebSocketSubscriber extends Subscriber<WebSocketInfo> {

    @CallSuper
    @Override
    public void onNext(@NonNull WebSocketInfo webSocketInfo) {
        if (webSocketInfo.isOnOpen()) {
            onOpen(webSocketInfo.getWebSocket());
        } else if (webSocketInfo.getString() != null) {
            onMessage(webSocketInfo.getString());
        } else if (webSocketInfo.getByteString() != null) {
            onMessage(webSocketInfo.getByteString());
        }
    }

    public abstract void onOpen(@NonNull WebSocket webSocket);

    public abstract void onMessage(@NonNull String text);

    public abstract void onMessage(@NonNull ByteString bytes);

    @Override
    public void onCompleted() {

    }

    @Override
    public void onError(Throwable e) {
        e.printStackTrace();
    }

}
