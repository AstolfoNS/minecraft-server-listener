package com.timeleafing.minecraft.security;

import com.timeleafing.minecraft.config.property.SecurityProperty;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
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

@Order(2)
@Component
@RequiredArgsConstructor
public class HmacAuthFilter extends OncePerRequestFilter {

    private final SecurityProperty props;
    // nonce 防重放（内存版）
    private final Map<String, Long> nonceCache = new ConcurrentHashMap<>();


    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/minecraft/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain chain
    ) throws ServletException, IOException {
        String tsHeader = props.getHeaderTs();
        String nonceHeader = props.getHeaderNonce();
        String signHeader = props.getHeaderSign();
        long maxSkewSeconds = props.getMaxSkewSeconds();

        String tsStr = request.getHeader(tsHeader);
        String nonce = request.getHeader(nonceHeader);
        String sign = request.getHeader(signHeader);

        if (tsStr == null || nonce == null || sign == null) {
            unauthorized(response, "Missing auth headers");
            return;
        }

        long ts;
        try {
            ts = Long.parseLong(tsStr);
        } catch (NumberFormatException e) {
            unauthorized(response, "Invalid timestamp");
            return;
        }

        long now = Instant.now().getEpochSecond();
        if (Math.abs(now - ts) > maxSkewSeconds) {
            unauthorized(response, "Timestamp expired");
            return;
        }

        // nonce 防重放
        cleanupOldNonces(now, maxSkewSeconds);
        if (nonceCache.putIfAbsent(nonce, ts) != null) {
            unauthorized(response, "Replay detected (nonce reused)");
            return;
        }

        String method = request.getMethod();
        String path = request.getRequestURI(); // 不含 query
        String canonical = method + "\n" + path + "\n" + tsStr + "\n" + nonce;

        byte[] secret = props.getHmacSecret().getBytes(StandardCharsets.UTF_8);
        String expected = hmacBase64(secret, canonical);

        if (!constantTimeEquals(expected, sign)) {
            unauthorized(response, "Bad signature");
            return;
        }

        chain.doFilter(request, response);
    }

    private void cleanupOldNonces(long now, long maxSkewSeconds) {
        nonceCache.entrySet().removeIf(e -> Math.abs(now - e.getValue()) > maxSkewSeconds);
    }

    private static void unauthorized(HttpServletResponse resp, String msg) throws IOException {
        resp.setStatus(401);
        resp.setContentType("text/plain; charset=utf-8");
        resp.getWriter().write("Unauthorized: " + msg);
    }

    private static String hmacBase64(byte[] secret, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            byte[] raw = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(raw);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) return false;
        byte[] x = a.getBytes(StandardCharsets.UTF_8);
        byte[] y = b.getBytes(StandardCharsets.UTF_8);
        if (x.length != y.length) return false;
        int r = 0;
        for (int i = 0; i < x.length; i++) r |= x[i] ^ y[i];
        return r == 0;
    }
}
