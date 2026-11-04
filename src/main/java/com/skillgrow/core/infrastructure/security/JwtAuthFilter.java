package com.skillgrow.core.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Exige un JWT válido en todas las rutas salvo las públicas. Es la segunda capa: APIM ya validó el
 * token, pero así el servicio no confía ciegamente en la red y se puede probar en local sin APIM.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final List<String> PUBLIC_EXACT = List.of(
            "/health", "/v1/health", "/v1/auth/register", "/v1/auth/login", "/v1/auth/google", "/swagger-ui.html");
    private static final List<String> PUBLIC_PREFIX = List.of("/v3/api-docs", "/swagger-ui/", "/actuator/health");

    private final TokenVerifier tokens;
    private final ErrorWriter errors;

    public JwtAuthFilter(TokenVerifier tokens, ErrorWriter errors) {
        this.tokens = tokens;
        this.errors = errors;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest req) {
        String path = req.getRequestURI();
        return "OPTIONS".equals(req.getMethod())
                || PUBLIC_EXACT.contains(path)
                || PUBLIC_PREFIX.stream().anyMatch(path::startsWith);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String header = req.getHeader("Authorization");
        Optional<CurrentUser> user = header != null && header.startsWith("Bearer ")
                ? tokens.verify(header.substring(7).trim())
                : Optional.empty();
        if (user.isEmpty()) {
            errors.write(req, res, 401, "UNAUTHORIZED", "Tu sesión expiró. Vuelve a iniciar sesión.");
            return;
        }
        req.setAttribute(CurrentUser.ATTRIBUTE, user.get());
        chain.doFilter(req, res);
    }
}
