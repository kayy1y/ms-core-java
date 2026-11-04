package com.skillgrow.core.infrastructure.adapter.out.catalog;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.skillgrow.core.domain.model.ServiceSnapshot;
import com.skillgrow.core.domain.port.out.ServiceCatalogPort;
import com.skillgrow.core.infrastructure.adapter.out.catalog.CatalogJson.ServiceJson;
import com.skillgrow.core.infrastructure.adapter.out.catalog.CatalogJson.SkillJson;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/**
 * Catálogo de prueba con los servicios y habilidades del seed de la Fase I (stub/catalog.json).
 * Sirve para desarrollar y probar MS1 sin depender de que MS2 esté desplegado.
 */
@Component
@ConditionalOnProperty(name = "app.catalog.mode", havingValue = "stub")
class StubServiceCatalog implements ServiceCatalogPort {

    private final Map<String, ServiceSnapshot> services;
    private final Map<String, String> skills;

    StubServiceCatalog(ObjectMapper json) throws IOException {
        try (InputStream in = new ClassPathResource("stub/catalog.json").getInputStream()) {
            StubFile file = json.readValue(in, StubFile.class);
            this.services = file.services().stream().map(ServiceJson::toSnapshot)
                    .collect(Collectors.toMap(ServiceSnapshot::id, Function.identity()));
            this.skills = file.skills().stream().collect(Collectors.toMap(SkillJson::id, SkillJson::name));
        }
    }

    @Override
    public Optional<ServiceSnapshot> findService(String serviceId) {
        return Optional.ofNullable(services.get(serviceId));
    }

    @Override
    public Map<String, String> skillNames(Collection<String> skillIds) {
        return skillIds.stream().filter(skills::containsKey).distinct()
                .collect(Collectors.toMap(Function.identity(), skills::get));
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record StubFile(List<ServiceJson> services, List<SkillJson> skills) {}
}
