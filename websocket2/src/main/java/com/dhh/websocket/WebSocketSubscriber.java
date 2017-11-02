package com.dhh.websocket;

import android.support.annotation.CallSuper;

import io.reactivex.Observer;
import io.reactivex.annotations.NonNull;
import okhttp3.WebSocket;
import okio.ByteString;

/**
 * Created by dhh on 2017/10/24.
 */

public abstract class WebSocketSubscriber implements Observer<WebSocketInfo> {
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
    public void onError(@NonNull Throwable e) {

    }

    @Override
    public void onComplete() {

    }

}
