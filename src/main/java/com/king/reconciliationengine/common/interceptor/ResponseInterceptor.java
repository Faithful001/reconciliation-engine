package com.king.reconciliationengine.common.interceptor;

import com.king.reconciliationengine.common.response.Response;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class ResponseInterceptor implements ResponseBodyAdvice<Object> {
    @Override
    public boolean supports(
            MethodParameter returnType,
            @NonNull Class<? extends HttpMessageConverter<?>> converterType
    ) {
        String declaringClassName = returnType.getDeclaringClass().getName();
        return !declaringClassName.startsWith("org.springdoc");
    }

    @Override
    public @Nullable Object beforeBodyWrite(
            @Nullable Object body,
            @NonNull MethodParameter returnType,
            @NonNull MediaType selectedContentType,
            @NonNull Class<? extends HttpMessageConverter<?>> selectedConverterType,
            @NonNull ServerHttpRequest request,
            @NonNull ServerHttpResponse response) {

        HttpServletRequest req = ((ServletServerHttpRequest) request).getServletRequest();
        HttpServletResponse res = ((ServletServerHttpResponse) response).getServletResponse();

        int currentStatus = res.getStatus();

        String message = null;
        Object data = null;
        Boolean success = null;

        if (body instanceof Response<?> api) {
            message = api.getMessage();
            data = api.getData();
            success = api.isSuccess();
        } else {
            data = body;
        }

        String finalMessage = message != null ? message : resolveDefaultMessage(currentStatus, req.getMethod());

        boolean finalSuccess = success != null ? success : currentStatus < 400;

        Map<String, Object> result = new LinkedHashMap<>();

        result.put("success", finalSuccess);
        result.put("message", finalMessage);
        result.put("data", data);

        return result;
    }

    private String resolveDefaultMessage(int status, String method) {
        if (status == 201 && method.equals("POST")) {
            return "Resource created successfully";
        }

        if (status >= 200 && status < 300) {
            return "Request successful";
        }

        return "Request failed";
    }

}
