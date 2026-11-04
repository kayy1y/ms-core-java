package com.skillgrow.core.infrastructure.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI de MS1 (/v3/api-docs). Se exporta a docs/openapi.yaml y se importa en APIM con el sufijo
 * "core": la ruta interna /v1/sessions queda pública como https://&lt;apim&gt;/core/v1/sessions.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI coreOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("SkillGrow · MS1 Core")
                        .version("1.0.0")
                        .description("Usuarios, login, disponibilidad y sesiones de mentoría. Fechas en epoch ms; "
                                + "días de calendario en YYYY-MM-DD. Errores: { status, code, message, details, "
                                + "timestampEpochMs, path }."))
                .components(new Components().addSecuritySchemes("bearer",
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList("bearer"));
    }
}
