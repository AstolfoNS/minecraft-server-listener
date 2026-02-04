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

    private String msg;

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

    public static <T> R<T> ok(String msg, T data) {
        return new R<>(HttpCode.OK.getCode(), msg, data, null);
    }

    public static <T> R<T> ok(HttpCode httpCode, T data) {
        return new R<>(httpCode.getCode(), httpCode.getMessage(), data, null);
    }

    public static <T> R<T> okWithMsg(String msg) {
        return ok(msg, null);
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

    public static <T> R<T> failed(String msg) {
        return failed(HttpCode.FAILED.getCode(), msg, null);
    }

    public static <T> R<T> failed(HttpCode httpCode) {
        return failed(httpCode, null);
    }

    public static <T> R<T> failed(HttpCode httpCode, T data) {
        return new R<>(httpCode.getCode(), httpCode.getMessage(), data, null);
    }

    public static <T> R<T> failed(int code, String msg, T data) {
        return new R<>(code, msg, data, null);
    }

    // ==========================
    // 带有详细信息的失败响应
    // ==========================
    public static <T> R<T> failedWithDetails(HttpCode httpCode, Object details) {
        return new R<>(httpCode.getCode(), httpCode.getMessage(), null, details);
    }

    public static <T> R<T> failedWithDetails(String msg, Object details) {
        return new R<>(HttpCode.FAILED.getCode(), msg, null, details);
    }
}
