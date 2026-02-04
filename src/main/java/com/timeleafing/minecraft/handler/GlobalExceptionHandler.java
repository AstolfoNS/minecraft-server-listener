package com.timeleafing.minecraft.handler;

import com.timeleafing.minecraft.common.response.R;
import com.timeleafing.minecraft.common.enumeration.HttpCode;
import com.timeleafing.minecraft.exception.BizException;
import com.timeleafing.minecraft.exception.DataNotFoundException;
import com.timeleafing.minecraft.exception.RateLimitedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    // 处理业务异常
    @ExceptionHandler(BizException.class)
    @ResponseStatus(HttpStatus.OK)  // 返回200 OK
    public R<Object> handleBizException(BizException ex) {
        // 记录日志
        log.error("Business exception occurred: {}", ex.getMessage(), ex);
        // 返回标准化响应
        return R.failed(HttpCode.OPERATION_FAILED.getCode(), ex.getMessage(), null);
    }

    // 处理限流异常
    @ExceptionHandler(RateLimitedException.class)
    @ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)  // 返回429 Too Many Requests
    public R<Object> handleRateLimitedException(RateLimitedException ex) {
        // 记录日志
        log.error("Rate limit exceeded: {}", ex.getMessage(), ex);
        // 返回限流响应
        return R.failed(HttpCode.RATE_LIMITED.getCode(), HttpCode.RATE_LIMITED.getMessage(), ex.getMessage());
    }

    // 处理数据未找到异常
    @ExceptionHandler(DataNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)  // 返回404 Not Found
    public R<Object> handleDataNotFoundException(DataNotFoundException ex) {
        // 记录日志
        log.error("Data not found: {}", ex.getMessage(), ex);
        // 返回数据未找到响应
        return R.failed(HttpCode.DATA_NOT_FOUND.getCode(), HttpCode.DATA_NOT_FOUND.getMessage(), ex.getMessage());
    }

    // 处理无效请求参数异常（BadRequest）
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)  // 返回400 Bad Request
    public R<Object> handleIllegalArgumentException(IllegalArgumentException ex) {
        // 记录日志
        log.error("Invalid argument: {}", ex.getMessage(), ex);
        // 返回无效请求响应
        return R.failed(HttpCode.BAD_REQUEST.getCode(), HttpCode.BAD_REQUEST.getMessage(), ex.getMessage());
    }

    // 处理其他未捕获的异常（通用异常）
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)  // 返回500 Internal Server Error
    public R<Object> handleException(Exception ex) {
        // 记录日志
        log.error("Unexpected error occurred: {}", ex.getMessage(), ex);
        // 返回通用错误响应
        return R.failed(HttpCode.FAILED.getCode(), HttpCode.FAILED.getMessage(), ex.getMessage());
    }
}
