package com.timeleafing.minecraft.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;

@Component
public class ApiKeyInterceptor implements HandlerInterceptor {

    @Value("#{securityProperty.headerName}")
    private String headerName;

    @Value("#{securityProperty.apiKey}")
    private String apiKey;


    @Override
    public boolean preHandle(
            HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull Object handler
    ) throws Exception {
        String provided = request.getHeader(headerName);
        if (!StringUtils.hasText(apiKey)) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getOutputStream().write("Server api-key not configured".getBytes(StandardCharsets.UTF_8));

            return false;
        }

        if (apiKey.equals(provided)) {
            return true;
        }

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("text/plain; charset=utf-8");
        response.getWriter().write("Unauthorized");

        return false;
    }
}
