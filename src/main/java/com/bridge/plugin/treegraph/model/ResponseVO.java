package com.bridge.plugin.treegraph.model;

public class ResponseVO<T> {
    public int code;
    public T data;
    public String message;

    public ResponseVO(T data) {
        this.data = data;
    }

    public static <T> ResponseVO<T> ok (T data) {
        return new ResponseVO<>(data);
    }

    public static <T> ResponseVO<T> fail (T data, int code, String message) {
        ResponseVO<T> v = new ResponseVO<>(data);
        v.code = code;
        v.message = message;
        return v;
    }
}
