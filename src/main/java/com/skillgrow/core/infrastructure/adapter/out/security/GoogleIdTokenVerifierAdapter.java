package com.skillgrow.core.infrastructure.adapter.out.security;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.skillgrow.core.domain.port.out.GoogleTokenVerifierPort;
import com.skillgrow.core.infrastructure.config.AppProperties;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Verifica el idToken de Google (firma, vencimiento y audiencia = nuestro client id). En local, con
 * GOOGLE_DEMO_ENABLED=true, el idToken "demo" entra con la cuenta GOOGLE_DEMO_EMAIL, igual que el mock.
 */
@Component
class GoogleIdTokenVerifierAdapter implements GoogleTokenVerifierPort {

    private static final Logger log = LoggerFactory.getLogger(GoogleIdTokenVerifierAdapter.class);
    private static final String DEMO_TOKEN = "demo";

    private final AppProperties.Google config;
    private final GoogleIdTokenVerifier verifier;

    GoogleIdTokenVerifierAdapter(AppProperties props) {
        this.config = props.google();
        String clientId = config.clientId();
        this.verifier = clientId == null || clientId.isBlank() ? null
                : new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), GsonFactory.getDefaultInstance())
                        .setAudience(List.of(clientId))
                        .build();
    }

    @Override
    public Optional<GoogleIdentity> verify(String idToken) {
        if (config.demoEnabled() && DEMO_TOKEN.equals(idToken)) {
            return Optional.of(new GoogleIdentity("demo-" + config.demoEmail(), config.demoEmail(), null, null));
        }
        if (verifier == null) {
            log.warn("Login con Google rechazado: falta GOOGLE_CLIENT_ID");
            return Optional.empty();
        }
        try {
            GoogleIdToken token = verifier.verify(idToken);
            if (token == null || !Boolean.TRUE.equals(token.getPayload().getEmailVerified())) {
                return Optional.empty();
            }
            GoogleIdToken.Payload p = token.getPayload();
            return Optional.of(new GoogleIdentity(p.getSubject(), p.getEmail(),
                    (String) p.get("name"), (String) p.get("picture")));
        } catch (Exception ex) {
            log.info("idToken de Google inválido: {}", ex.getMessage());
            return Optional.empty();
        }
    }
}
