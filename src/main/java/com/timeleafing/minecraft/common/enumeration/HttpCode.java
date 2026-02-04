package com.timeleafing.minecraft.common.enumeration;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 通用 HTTP 与业务响应码枚举
 *
 * <p>既包含标准 HTTP 状态码（2xx、4xx、5xx），
 * 也支持业务层语义化响应（如 LOGIN_EXPIRED、RATE_LIMITED 等）。
 * </p>
 */
@AllArgsConstructor
@Getter
public enum HttpCode {

    // ========== 2xx 成功 ==========
    OK(200, "操作成功"),
    CREATED(201, "资源已创建"),
    ACCEPTED(202, "请求已接受"),
    NO_CONTENT(204, "无返回内容"),

    // ========== 4xx 客户端错误 ==========
    BAD_REQUEST(400, "错误的请求参数"),
    UNAUTHORIZED(401, "未授权访问"),
    FORBIDDEN(403, "没有访问权限"),
    NOT_FOUND(404, "资源未找到"),
    METHOD_NOT_ALLOWED(405, "请求方法不被允许"),
    TOO_MANY_REQUESTS(429, "请求过于频繁，请稍后重试"),

    // ========== 5xx 服务端错误 ==========
    FAILED(500, "服务器内部错误"),
    BAD_GATEWAY(502, "网关错误"),
    SERVICE_UNAVAILABLE(503, "服务暂不可用"),
    GATEWAY_TIMEOUT(504, "网关超时"),

    // ========== 业务扩展 ==========
    VALIDATION_ERROR(901, "参数校验失败"),
    LOGIN_EXPIRED(902, "登录状态已过期"),
    TOKEN_INVALID(903, "无效的Token"),
    PERMISSION_DENIED(904, "权限不足"),
    RATE_LIMITED(905, "触发限流，请稍后再试"),
    DATA_NOT_FOUND(906, "数据不存在"),
    OPERATION_FAILED(907, "操作失败"),
    DUPLICATE_REQUEST(908, "重复请求，请稍后再试");


    private final Integer code;

    private final String message;
}
