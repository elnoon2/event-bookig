package com.badyauniversity.eventbooking.qrpass.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Per-IP fixed-window rate limiter for the QR validation endpoint, to blunt brute-force / abuse of
 * {@code POST /api/qr-pass/validate}. In-memory and best-effort: for a multi-instance deployment move
 * this to a shared store (e.g. Redis) — see QR_VALIDATION_SYSTEM.md.
 */
@Component
@Order(1)
public class QrRateLimitFilter extends OncePerRequestFilter {

    private static final String TARGET = "/api/qr-pass/validate";
    private static final long WINDOW_MS = 60_000L;

    private final QrPassProperties props;
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    public QrRateLimitFilter(QrPassProperties props) {
        this.props = props;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !TARGET.equals(request.getRequestURI()) || !"POST".equalsIgnoreCase(request.getMethod());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String ip = clientIp(request);
        if (isOverLimit(ip)) {
            response.setStatus(429); // Too Many Requests
            response.setContentType("application/json");
            response.getWriter().write("{\"status\":\"RATE_LIMITED\",\"message\":\"Too many scan attempts. Please slow down.\"}");
            return;
        }
        filterChain.doFilter(request, response);
    }

    private boolean isOverLimit(String ip) {
        long now = System.currentTimeMillis();
        Window w = windows.compute(ip, (k, existing) -> {
            if (existing == null || now - existing.windowStart >= WINDOW_MS) {
                return new Window(now);
            }
            return existing;
        });
        int count = w.count.incrementAndGet();
        return count > props.getRateLimitPerMinute();
    }

    public static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private static final class Window {
        final long windowStart;
        final AtomicInteger count = new AtomicInteger(0);

        Window(long windowStart) {
            this.windowStart = windowStart;
        }
    }
}
