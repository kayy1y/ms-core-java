package com.skillgrow.core.infrastructure.config;

import com.skillgrow.core.infrastructure.security.AdminInterceptor;
import com.skillgrow.core.infrastructure.security.CurrentUserArgumentResolver;
import java.util.List;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final CurrentUserArgumentResolver currentUser;
    private final AdminInterceptor admin;

    public WebConfig(CurrentUserArgumentResolver currentUser, AdminInterceptor admin) {
        this.currentUser = currentUser;
        this.admin = admin;
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(currentUser);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(admin).addPathPatterns("/v1/admin/**");
    }
}
