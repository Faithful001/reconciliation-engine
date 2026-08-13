package com.king.reconciliationengine.common.response;

import lombok.Getter;

@Getter
public class Response<T> {
    private boolean success;
    private String message;
    private T data;

    public static <T> Response<T> success(String message, T data) {
        Response<T> response = new Response<>();

        response.success = true;
        response.message = message;
        response.data = data;

        return response;
    }

    public  static <T> Response<T> error(String message) {
        Response<T> response = new Response<>();

        response.success = false;
        response.message = message;
        response.data = null;

        return response;
    }
}
