package com.skillgrow.core.infrastructure.adapter.out.catalog;

import com.skillgrow.core.domain.exception.ServiceUnavailableException;
import com.skillgrow.core.domain.model.ServiceSnapshot;
import com.skillgrow.core.domain.port.out.ServiceCatalogPort;
import com.skillgrow.core.infrastructure.adapter.out.catalog.CatalogJson.ServiceJson;
import com.skillgrow.core.infrastructure.adapter.out.catalog.CatalogJson.SkillJson;
import com.skillgrow.core.infrastructure.config.AppProperties;
import com.skillgrow.core.infrastructure.security.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Clock;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Llama a MS2 a través de APIM (los backends solo aceptan tráfico de APIM). Reenvía el JWT del usuario
 * y el X-Correlation-Id. Timeout corto: si MS2 no responde, 503 en lugar de dejar al usuario esperando.
 */
@Component
@ConditionalOnProperty(name = "app.catalog.mode", havingValue = "http", matchIfMissing = true)
class HttpServiceCatalogClient implements ServiceCatalogPort {

    private static final Logger log = LoggerFactory.getLogger(HttpServiceCatalogClient.class);
    private static final Duration SKILLS_CACHE = Duration.ofMinutes(10);

    private final RestClient http;
    private final Clock clock;
    private volatile Map<String, String> skillsCache = Map.of();
    private volatile long skillsLoadedAt;

    HttpServiceCatalogClient(AppProperties props, Clock clock) {
        AppProperties.Catalog c = props.catalog();
        if (c.baseUrl() == null || c.baseUrl().isBlank()) {
            throw new IllegalStateException("SERVICE_CATALOG_MODE=http necesita DOMINIO_BASE_URL (https://<apim>/dominio)");
        }
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(c.timeoutMs());
        factory.setReadTimeout(c.timeoutMs());
        RestClient.Builder builder = RestClient.builder().baseUrl(c.baseUrl()).requestFactory(factory);
        if (c.subscriptionKey() != null && !c.subscriptionKey().isBlank()) {
            builder.defaultHeader("Ocp-Apim-Subscription-Key", c.subscriptionKey());
        }
        this.http = builder.build();
        this.clock = clock;
    }

    @Override
    public Optional<ServiceSnapshot> findService(String serviceId) {
        try {
            ServiceJson body = http.get().uri("/v1/services/{id}", serviceId)
                    .headers(this::forwardCaller)
                    .retrieve()
                    .body(ServiceJson.class);
            return Optional.ofNullable(body).map(ServiceJson::toSnapshot);
        } catch (HttpClientErrorException.NotFound ex) {
            return Optional.empty();
        } catch (RestClientException ex) {
            throw unavailable(ex);
        }
    }

    @Override
    public Map<String, String> skillNames(Collection<String> skillIds) {
        Map<String, String> all = skills();
        return skillIds.stream().filter(all::containsKey).distinct()
                .collect(Collectors.toMap(Function.identity(), all::get));
    }

    private Map<String, String> skills() {
        if (!skillsCache.isEmpty() && clock.millis() - skillsLoadedAt < SKILLS_CACHE.toMillis()) {
            return skillsCache;
        }
        try {
            SkillJson[] body = http.get().uri("/v1/catalog/skills").headers(this::forwardCaller)
                    .retrieve().body(SkillJson[].class);
            skillsCache = Arrays.stream(body == null ? new SkillJson[0] : body)
                    .collect(Collectors.toMap(SkillJson::id, SkillJson::name, (a, b) -> a));
            skillsLoadedAt = clock.millis();
            return skillsCache;
        } catch (RestClientException ex) {
            throw unavailable(ex);
        }
    }

    private void forwardCaller(HttpHeaders headers) {
        String correlationId = CorrelationIdFilter.current();
        if (correlationId != null) {
            headers.set(CorrelationIdFilter.HEADER, correlationId);
        }
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
            HttpServletRequest req = attrs.getRequest();
            String auth = req.getHeader(HttpHeaders.AUTHORIZATION);
            if (auth != null) {
                headers.set(HttpHeaders.AUTHORIZATION, auth);
            }
        }
    }

    private static ServiceUnavailableException unavailable(RestClientException ex) {
        log.warn("MS2 no respondió: {}", ex.getMessage());
        return new ServiceUnavailableException("No pudimos consultar los servicios en este momento. Intenta de nuevo en unos minutos.");
    }
}
