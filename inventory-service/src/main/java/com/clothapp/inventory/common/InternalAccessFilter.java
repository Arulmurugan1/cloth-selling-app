package com.clothapp.inventory.common;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Set;

/** Accepts only calls from the gateway / sibling services and enforces admin-only writes. */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class InternalAccessFilter extends OncePerRequestFilter {

    private static final Set<String> ADMIN_ROLES = Set.of("ADMIN", "SUPER_ADMIN");

    private final byte[] internalKey;

    public InternalAccessFilter(@Value("${app.internal-api-key}") String internalKey) {
        this.internalKey = internalKey.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getRequestURI().startsWith("/actuator");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String provided = request.getHeader("X-Internal-Key");
        if (provided == null || !MessageDigest.isEqual(internalKey, provided.getBytes(StandardCharsets.UTF_8))) {
            reject(response, HttpServletResponse.SC_UNAUTHORIZED, "Missing or invalid internal key");
            return;
        }
        String path = request.getRequestURI();
        boolean write = !"GET".equals(request.getMethod()) && !"HEAD".equals(request.getMethod());
        boolean adminArea = path.startsWith("/api/batch") || (write && path.startsWith("/api/products"));
        if (adminArea && !ADMIN_ROLES.contains(request.getHeader("X-User-Role"))) {
            reject(response, HttpServletResponse.SC_FORBIDDEN, "Admin role required");
            return;
        }
        chain.doFilter(request, response);
    }

    private void reject(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"message\":\"" + message + "\"}");
    }
}
