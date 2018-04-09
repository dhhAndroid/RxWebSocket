package com.dhh.websocket;

import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

/**
 * Created by dhh on 2018/4/9.
 * <p>
 * 功能：可根据传入的泛型，直接将String类型的text转化成实体类
 * </P>
 *
 * @author dhh
 */
public abstract class WebSocketSubscriber2<T> extends WebSocketSubscriber {
    private static final Gson GSON = new Gson();
    protected Type type;


    public WebSocketSubscriber2() {
        analysisType();
    }

    private void analysisType() {
        Type superclass = getClass().getGenericSuperclass();
        if (superclass instanceof Class) {
            throw new RuntimeException("No generics found!");
        }
        ParameterizedType type = (ParameterizedType) superclass;
        this.type = type.getActualTypeArguments()[0];
    }

    @Override
    @CallSuper
    protected void onMessage(@NonNull String text) {
        Observable.just(text)
                .map(new Func1<String, T>() {
                    @Override
                    public T call(String s) {
                        try {
                            return GSON.fromJson(s, type);
                        } catch (JsonSyntaxException e) {
                            return GSON.fromJson(GSON.fromJson(s, String.class), type);
                        }
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<T>() {
                    @Override
                    public void call(T t) {
                        onMessage(t);
                    }
                });
    }

    protected abstract void onMessage(T t);
}
