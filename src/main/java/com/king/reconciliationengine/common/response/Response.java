package com.king.reconciliationengine.common.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "Standard API Response Wrapper")
public class Response<T> {
    @Schema(description = "Indicates if the request was successful", example = "true")
    private boolean success;

    @Schema(description = "Response message or outcome description", example = "Operation completed successfully")
    private String message;

    @Schema(description = "Response data payload")
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
