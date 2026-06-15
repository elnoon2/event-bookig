package com.badyauniversity.eventbooking.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Blocks direct download of sensitive project files.
 *
 * The frontend is served from the repository root via a {@code /**} static resource handler
 * (see {@link WebConfig}), which would otherwise expose source code, the SQL schema, build files,
 * docs and credential-bearing helpers (e.g. database_schema.sql, test_user.json, pom.xml,
 * the backend/ and whatsapp-service/ trees). This filter denies those paths before the static
 * handler can serve them, while leaving the real frontend assets and the /api, /uploads and
 * /h2-console routes untouched. Denials return 404 so file existence is not confirmed.
 */
@Component
@Order(0)
public class StaticContentSecurityFilter extends OncePerRequestFilter {

    private static final List<String> ALLOWED_PREFIXES = List.of(
            "/api/", "/uploads/", "/h2-console");

    private static final List<String> BLOCKED_PREFIXES = List.of(
            "/backend", "/whatsapp-service", "/.git", "/.vscode", "/.idea",
            "/node_modules", "/target", "/src");

    private static final List<String> BLOCKED_SUFFIXES = List.of(
            ".sql", ".properties", ".bat", ".sh", ".md", ".xml", ".json", ".lock",
            ".iml", ".classpath", ".project", ".log", ".env", ".jar", ".class");

    private static final List<String> BLOCKED_EXACT = List.of(
            "/server.js", "/models", "/.gitignore");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = decode(request.getRequestURI());
        if (path == null) {
            path = "/";
        }
        String lower = path.toLowerCase().replace('\\', '/');

        // Dynamic endpoints and intentionally-served directories are never filtered.
        for (String allowed : ALLOWED_PREFIXES) {
            if (lower.startsWith(allowed)) {
                filterChain.doFilter(request, response);
                return;
            }
        }

        if (isBlocked(lower)) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isBlocked(String lower) {
        for (String prefix : BLOCKED_PREFIXES) {
            if (lower.equals(prefix) || lower.startsWith(prefix + "/")) {
                return true;
            }
        }
        for (String exact : BLOCKED_EXACT) {
            if (lower.equals(exact)) {
                return true;
            }
        }
        for (String suffix : BLOCKED_SUFFIXES) {
            if (lower.endsWith(suffix)) {
                return true;
            }
        }
        return false;
    }

    private static String decode(String value) {
        if (value == null) {
            return null;
        }
        try {
            return URLDecoder.decode(value, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            return value;
        }
    }
}
