package com.timeleafing.minecraft.common.response;

import com.timeleafing.minecraft.common.enumeration.HttpCode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@AllArgsConstructor
@Builder
@Data
public class R<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private int code;

    private String message;

    private T data;

    private Object details;

    // ==========================
    // 成功响应
    // ==========================
    public static <T> R<T> ok() {
        return ok(null);
    }

    public static <T> R<T> ok(T data) {
        return new R<>(HttpCode.OK.getCode(), HttpCode.OK.getMessage(), data, null);
    }

    public static <T> R<T> ok(String message, T data) {
        return new R<>(HttpCode.OK.getCode(), message, data, null);
    }

    public static <T> R<T> ok(HttpCode httpCode, T data) {
        return new R<>(httpCode.getCode(), httpCode.getMessage(), data, null);
    }

    public static <T> R<T> okWithMsg(String message) {
        return ok(message, null);
    }

    // ==========================
    // 失败响应
    // ==========================
    public static <T> R<T> failed() {
        return failed(HttpCode.FAILED, null);
    }

    public static <T> R<T> failed(T data) {
        return failed(HttpCode.FAILED, data);
    }

    public static <T> R<T> failed(String message) {
        return failed(HttpCode.FAILED.getCode(), message, null);
    }

    public static <T> R<T> failed(HttpCode httpCode) {
        return failed(httpCode, null);
    }

    public static <T> R<T> failed(HttpCode httpCode, T data) {
        return new R<>(httpCode.getCode(), httpCode.getMessage(), data, null);
    }

    public static <T> R<T> failed(int code, String message, T data) {
        return new R<>(code, message, data, null);
    }

    // ==========================
    // 带有详细信息的失败响应
    // ==========================
    public static <T> R<T> failedWithDetails(HttpCode httpCode, Object details) {
        return new R<>(httpCode.getCode(), httpCode.getMessage(), null, details);
    }

    public static <T> R<T> failedWithDetails(String message, Object details) {
        return new R<>(HttpCode.FAILED.getCode(), message, null, details);
    }
}
