package com.dhh.websocket;

import android.support.annotation.Nullable;

import okhttp3.WebSocket;
import okio.ByteString;

/**
 * Created by dhh on 2017/9/21.
 */

public class WebSocketInfo {
    private WebSocket mWebSocket;
    private String mString;
    private ByteString mByteString;
    private boolean onOpen;
    private boolean onReconnect;

    private WebSocketInfo() {
    }

    WebSocketInfo(WebSocket webSocket, boolean onOpen) {
        mWebSocket = webSocket;
        this.onOpen = onOpen;
    }

    WebSocketInfo(WebSocket webSocket, String mString) {
        mWebSocket = webSocket;
        this.mString = mString;
    }

    WebSocketInfo(WebSocket webSocket, ByteString byteString) {
        mWebSocket = webSocket;
        mByteString = byteString;
    }

    static WebSocketInfo createReconnect() {
        WebSocketInfo socketInfo = new WebSocketInfo();
        socketInfo.onReconnect = true;
        return socketInfo;
    }

    public WebSocket getWebSocket() {
        return mWebSocket;
    }

    public void setWebSocket(WebSocket webSocket) {
        mWebSocket = webSocket;
    }

    @Nullable
    public String getString() {
        return mString;
    }

    public void setString(String string) {
        this.mString = string;
    }

    @Nullable
    public ByteString getByteString() {
        return mByteString;
    }

    public void setByteString(ByteString byteString) {
        mByteString = byteString;
    }

    public boolean isOnOpen() {
        return onOpen;
    }

    public boolean isOnReconnect() {
        return onReconnect;
    }
}
