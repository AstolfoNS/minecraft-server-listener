package com.timeleafing.minecraft.common.constant;

/**
 * HTTP 状态码常量定义类
 *
 * <p>用于统一管理项目中常用的 HTTP 状态码，避免魔法数字。</p>
 *
 * <p>说明：
 * - 与 {@code org.springframework.http.HttpStatus} 一致；
 * </p>
 */
public interface HttpStatusConst {

    // --- 1xx Informational ---
    int CONTINUE = 100;
    int SWITCHING_PROTOCOLS = 101;
    int PROCESSING = 102;

    // --- 2xx Success ---
    int OK = 200;
    int CREATED = 201;
    int ACCEPTED = 202;
    int NON_AUTHORITATIVE_INFORMATION = 203;
    int NO_CONTENT = 204;
    int RESET_CONTENT = 205;
    int PARTIAL_CONTENT = 206;

    // --- 3xx Redirection ---
    int MULTIPLE_CHOICES = 300;
    int MOVED_PERMANENTLY = 301;
    int FOUND = 302;
    int SEE_OTHER = 303;
    int NOT_MODIFIED = 304;
    int TEMPORARY_REDIRECT = 307;
    int PERMANENT_REDIRECT = 308;

    // --- 4xx Client Error ---
    int BAD_REQUEST = 400;
    int UNAUTHORIZED = 401;
    int FORBIDDEN = 403;
    int NOT_FOUND = 404;
    int METHOD_NOT_ALLOWED = 405;
    int NOT_ACCEPTABLE = 406;
    int REQUEST_TIMEOUT = 408;
    int CONFLICT = 409;
    int GONE = 410;
    int PAYLOAD_TOO_LARGE = 413;
    int URI_TOO_LONG = 414;
    int UNSUPPORTED_MEDIA_TYPE = 415;
    int TOO_MANY_REQUESTS = 429;
    int REQUEST_HEADER_FIELDS_TOO_LARGE = 431;

    // --- 5xx Server Error ---
    int INTERNAL_SERVER_ERROR = 500;
    int NOT_IMPLEMENTED = 501;
    int BAD_GATEWAY = 502;
    int SERVICE_UNAVAILABLE = 503;
    int GATEWAY_TIMEOUT = 504;
    int HTTP_VERSION_NOT_SUPPORTED = 505;

}
