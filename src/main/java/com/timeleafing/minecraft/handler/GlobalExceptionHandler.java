package com.timeleafing.minecraft.handler;

import com.timeleafing.minecraft.common.enumeration.HttpCode;
import com.timeleafing.minecraft.common.response.R;
import com.timeleafing.minecraft.exception.BizException;
import com.timeleafing.minecraft.exception.DataNotFoundException;
import com.timeleafing.minecraft.exception.RateLimitedException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ===================== 业务异常 =====================

    @ExceptionHandler(BizException.class)
    public ResponseEntity<R<Object>> handleBizException(BizException ex) {
        log.warn("BizException: {}", ex.getMessage());

        return okFail(ex.getMessage());
    }

    @ExceptionHandler(RateLimitedException.class)
    public ResponseEntity<R<Object>> handleRateLimitedException(RateLimitedException ex) {
        log.warn("RateLimited: {}", ex.getMessage());

        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(R.failed(HttpCode.RATE_LIMITED.getCode(), HttpCode.RATE_LIMITED.getMessage(), ex.getMessage()));
    }

    @ExceptionHandler(DataNotFoundException.class)
    public ResponseEntity<R<Object>> handleDataNotFoundException(DataNotFoundException ex) {
        log.warn("DataNotFound: {}", ex.getMessage());

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(R.failed(HttpCode.DATA_NOT_FOUND.getCode(), HttpCode.DATA_NOT_FOUND.getMessage(), ex.getMessage()));
    }

    /**
     * 409 冲突/非法状态
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<R<Object>> handleIllegalState(IllegalStateException ex) {
        log.warn("IllegalState: {}", ex.getMessage());

        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(R.failed(HttpCode.OPERATION_FAILED.getCode(), ex.getMessage(), null));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<R<Object>> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("IllegalArgument: {}", ex.getMessage());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(R.failed(HttpCode.BAD_REQUEST.getCode(), HttpCode.BAD_REQUEST.getMessage(), ex.getMessage()));
    }

    // ===================== 参数校验相关 =====================

    /**
     * @RequestBody + @Valid 校验失败
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<R<Object>> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        String msg = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining("; "));

        log.warn("Validation failed: {}", msg);

        return ResponseEntity.badRequest()
                .body(R.failed(HttpCode.BAD_REQUEST.getCode(), "Validation failed", msg));
    }

    /**
     * @ModelAttribute / @RequestParam 等绑定校验失败
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<R<Object>> handleBindException(BindException ex) {
        String msg = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining("; "));

        log.warn("Bind failed: {}", msg);

        return ResponseEntity.badRequest()
                .body(R.failed(HttpCode.BAD_REQUEST.getCode(), "Bind failed", msg));
    }

    /**
     * JSON 解析失败 / body 缺失
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<R<Object>> handleNotReadable(HttpMessageNotReadableException ex) {
        // 不要刷堆栈，属于客户端请求错误
        ex.getMostSpecificCause();

        String msg = ex.getMostSpecificCause().getMessage();

        log.warn("Request body not readable: {}", msg);

        return ResponseEntity.badRequest()
                .body(R.failed(HttpCode.BAD_REQUEST.getCode(), "Request body not readable", msg));
    }

    /**
     * 缺少请求参数
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<R<Object>> handleMissingParam(MissingServletRequestParameterException ex) {
        log.warn("Missing param: {}", ex.getMessage());

        return ResponseEntity.badRequest()
                .body(R.failed(HttpCode.BAD_REQUEST.getCode(), "Missing request parameter", ex.getMessage()));
    }

    /**
     * 参数类型不匹配：/xxx/{id} 传了非数字
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<R<Object>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String msg = "Parameter '" + ex.getName() + "' type mismatch";

        log.warn("{}: {}", msg, ex.getMessage());

        return ResponseEntity.badRequest()
                .body(R.failed(HttpCode.BAD_REQUEST.getCode(), msg, ex.getMessage()));
    }

    // ===================== 路由/协议相关 =====================

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<R<Object>> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        log.warn("Method not supported: {}", ex.getMessage());

        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(R.failed(HttpCode.BAD_REQUEST.getCode(), "Method not allowed", ex.getMessage()));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<R<Object>> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex) {
        log.warn("Media type not supported: {}", ex.getMessage());

        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body(R.failed(HttpCode.BAD_REQUEST.getCode(), "Unsupported media type", ex.getMessage()));
    }

    /**
     * 单独接住，避免掉进 generic 造成刷堆栈
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<R<Object>> handleNoResource(NoResourceFoundException ex) {
        log.warn("NoResourceFound: {}", ex.getMessage());

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(R.failed(404, "Not Found", ex.getMessage()));
    }

    // ===================== 兜底系统异常 =====================

    @ExceptionHandler(Exception.class)
    public ResponseEntity<R<Object>> handleException(Exception ex, HttpServletRequest req) {
        // 系统异常才打印堆栈，并给一个 traceId 方便查日志
        String traceId = UUID.randomUUID().toString().replace("-", "");

        log.error("Unexpected error (traceId={}, path={}): {}", traceId, req.getRequestURI(), ex.getMessage(), ex);

        // 返回给前端不要暴露堆栈；traceId 给排查用
        String safeMsg = "Internal server error, traceId=" + traceId;

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(R.failed(HttpCode.FAILED.getCode(), HttpCode.FAILED.getMessage(), safeMsg));
    }

    // ===================== helpers =====================

    private static ResponseEntity<R<Object>> okFail(String msg) {
        return ResponseEntity.ok(R.failed(HttpCode.OPERATION_FAILED.getCode(), msg, null));
    }
}
