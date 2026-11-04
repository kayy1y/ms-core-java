package com.skillgrow.core.infrastructure.security;

import com.skillgrow.core.domain.exception.ForbiddenException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/** /v1/admin/** solo para rol admin (APIM también lo exige con required-claims). */
@Component
public class AdminInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse res, Object handler) {
        Object user = req.getAttribute(CurrentUser.ATTRIBUTE);
        if (!(user instanceof CurrentUser current) || !current.isAdmin()) {
            throw new ForbiddenException("Esta sección es solo para administradores.");
        }
        return true;
    }
}
