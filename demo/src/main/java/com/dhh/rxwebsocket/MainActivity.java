package com.dhh.rxwebsocket;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.dhh.websocket.RxWebSocketUtil;
import com.dhh.websocket.WebSocketInfo;

import okhttp3.WebSocket;
import okio.ByteString;
import rx.Subscription;
import rx.functions.Action1;

public class MainActivity extends AppCompatActivity {

    private WebSocket mWebSocket;
    private EditText editText;
    private Button send;
    private Button centect;
    private Subscription mSubscription;
    private TextView textview;
    private String url;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        //from : http://www.blue-zero.com/WebSocket/     .  thanks
        url = "ws://121.40.165.18:8088";

        //if you want to set your okhttpClient
//        OkHttpClient yourClient = new OkHttpClient();
//        RxWebSocketUtil.getInstance().setClient(yourClient);


        centect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mSubscription != null) return;
                //注意取消订阅,有多种方式,比如 rxlifecycle
                mSubscription = RxWebSocketUtil.getInstance().getWebSocketInfo(url)
                        .subscribe(new Action1<WebSocketInfo>() {
                            @Override
                            public void call(WebSocketInfo webSocketInfo) {
                                mWebSocket = webSocketInfo.getWebSocket();
                                Log.d("MainActivity", webSocketInfo.getString());
                                Log.d("MainActivity", "webSocketInfo.getByteString():" + webSocketInfo.getByteString());
                                textview.setText(webSocketInfo.getString());
                            }
                        });

            }
        });
        //send message
        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mWebSocket != null) {
                    mWebSocket.send(editText.getText().toString());
                } else {
                    Toast.makeText(MainActivity.this, "WebSocket ont contect", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    public void otherUse() {
        //getWebSocket
        RxWebSocketUtil.getInstance().getWebSocket(url)
                .subscribe(new Action1<WebSocket>() {
                    @Override
                    public void call(WebSocket webSocket) {
                    }
                });
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
        super.onDestroy();
        if (mSubscription != null) {
            mSubscription.unsubscribe();
        }
    }
}
