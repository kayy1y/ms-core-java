package com.skillgrow.core.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Toda la configuración propia del servicio. Los valores salen de variables de entorno (ver application.yml). */
@ConfigurationProperties(prefix = "app")
public record AppProperties(
        String zone,
        Jwt jwt,
        Google google,
        Events events,
        Apim apim,
        Catalog catalog) {

    public record Jwt(String secret, int ttlHours, String issuer) {}

    public record Google(String clientId, boolean demoEnabled, String demoUserId) {}

    public record Events(boolean enabled, String connection, String queue) {}

    public record Apim(boolean certCheck, String certThumbprint) {}

    /** mode = stub (servicios del seed de la Fase I) | http (MS2 a través de APIM). */
    public record Catalog(String mode, String baseUrl, String subscriptionKey, int timeoutMs) {}
}
