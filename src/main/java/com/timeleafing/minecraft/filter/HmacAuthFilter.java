package com.timeleafing.minecraft.filter;

import com.timeleafing.minecraft.config.property.SecurityProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RequiredArgsConstructor
@Component
public class HmacAuthFilter extends OncePerRequestFilter {

    private final SecurityProperties props;
    // nonce 防重放（内存版）
    private final Map<String, Long> nonceCache = new ConcurrentHashMap<>();

    private static final String HMAC_SHA256 = "HmacSHA256";

    /**
     * 判断是否是 WebSocket 握手请求
     */
    private static boolean isWebSocketHandshake(HttpServletRequest req) {
        return "websocket".equalsIgnoreCase(req.getHeader("Upgrade")) && req.getHeader("Connection").toLowerCase(java.util.Locale.ROOT).contains("upgrade");
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        // 如果是 WebSocket 握手请求，直接放行
        return isWebSocketHandshake(request) || !request.getRequestURI().startsWith(request.getContextPath() + "/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain chain
    ) throws ServletException, IOException {
        // 获取头部参数
        String tsStr = request.getHeader(props.getHeaderTs());
        String nonce = request.getHeader(props.getHeaderNonce());
        String sign = request.getHeader(props.getHeaderSign());

        // 检查必需的请求头是否存在
        if (tsStr == null || nonce == null || sign == null) {
            unauthorized(response, "Missing auth headers");
            return;
        }
        // 解析时间戳并校验
        long ts;
        try {
            ts = Long.parseLong(tsStr);
        } catch (NumberFormatException e) {
            unauthorized(response, "Invalid timestamp");
            return;
        }
        long now = Instant.now().getEpochSecond();
        if (Math.abs(now - ts) > props.getMaxSkewSeconds()) {
            unauthorized(response, "Timestamp expired");
            return;
        }
        // 防重放：检查 nonce 是否重复
        if (isReplay(nonce, ts)) {
            unauthorized(response, "Replay detected (nonce reused)");
            return;
        }
        // 生成预期的签名并与请求中的签名进行比对
        String method = request.getMethod();
        String path = request.getRequestURI();
        String canonical = method + "\n" + path + "\n" + tsStr + "\n" + nonce;

        String expectedSign = hmacBase64(props.getHmacSecret().getBytes(StandardCharsets.UTF_8), canonical);

        // 比较签名
        if (!constantTimeEquals(expectedSign, sign)) {
            unauthorized(response, "Bad signature");
            return;
        }
        // 通过验证，继续处理请求
        chain.doFilter(request, response);
    }

    /**
     * 防止重放攻击：如果 nonce 在缓存中已存在，则拒绝
     */
    private boolean isReplay(String nonce, long ts) {
        cleanupOldNonces(Instant.now().getEpochSecond());
        return nonceCache.putIfAbsent(nonce, ts) != null;
    }

    /**
     * 清理过期的 nonce 缓存
     */
    private void cleanupOldNonces(long currentTime) {
        nonceCache.entrySet().removeIf(entry -> Math.abs(currentTime - entry.getValue()) > props.getMaxSkewSeconds());
    }

    /**
     * 对外返回 401 Unauthorized 错误
     */
    private static void unauthorized(HttpServletResponse resp, String msg) throws IOException {
        resp.setStatus(401);
        resp.setContentType("text/plain; charset=utf-8");
        resp.getWriter().write("Unauthorized: " + msg);
    }

    /**
     * 使用 HMAC-SHA256 算法生成签名
     */
    private static String hmacBase64(byte[] secret, String data) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(secret, HMAC_SHA256));
            byte[] raw = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(raw);
        } catch (Exception e) {
            throw new RuntimeException("HMAC-SHA256 error", e);
        }
    }

    /**
     * 常量时间比较字符串，防止时间攻击
     */
    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        byte[] x = a.getBytes(StandardCharsets.UTF_8);
        byte[] y = b.getBytes(StandardCharsets.UTF_8);
        if (x.length != y.length) {
            return false;
        }
        int r = 0;
        for (int i = 0; i < x.length; i++) {
            r |= x[i] ^ y[i];
        }
        return r == 0;
    }
}
