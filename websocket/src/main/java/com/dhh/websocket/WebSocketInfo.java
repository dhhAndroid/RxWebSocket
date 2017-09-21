package com.dhh.websocket;

import okhttp3.WebSocket;
import okio.ByteString;

/**
 * Created by dhh on 2017/9/21.
 */

public class WebSocketInfo implements Cloneable {
    private WebSocket mWebSocket;
    private String mString;
    private ByteString mByteString;

    public WebSocketInfo() {
    }

    public WebSocketInfo(WebSocket webSocket, String mString) {
        mWebSocket = webSocket;
        this.mString = mString;
    }

    public WebSocketInfo(WebSocket webSocket, ByteString byteString) {
        mWebSocket = webSocket;
        mByteString = byteString;
    }

    public WebSocket getWebSocket() {
        return mWebSocket;
    }

    public void setWebSocket(WebSocket webSocket) {
        mWebSocket = webSocket;
    }

    public String getString() {
        return mString;
    }

    public void setString(String string) {
        this.mString = string;
    }

    public ByteString getByteString() {
        return mByteString;
    }

    public void setByteString(ByteString byteString) {
        mByteString = byteString;
    }
}
