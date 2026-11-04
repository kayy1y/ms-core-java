package com.skillgrow.core.infrastructure.adapter.out.security;

import com.skillgrow.core.domain.model.Role;
import com.skillgrow.core.domain.model.User;
import com.skillgrow.core.domain.model.WireEnum;
import com.skillgrow.core.domain.port.out.TokenIssuerPort;
import com.skillgrow.core.infrastructure.config.AppProperties;
import com.skillgrow.core.infrastructure.security.CurrentUser;
import com.skillgrow.core.infrastructure.security.TokenVerifier;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

/**
 * Emite y valida el JWT HS256 de SkillGrow. APIM valida el mismo token con validate-jwt y la misma
 * clave (Named value jwt-signing-key, en base64); este servicio lo vuelve a validar como segunda capa.
 */
@Component
public class JwtTokenService implements TokenIssuerPort, TokenVerifier {

    private static final int MAX_AVATAR_IN_TOKEN = 500;

    private final SecretKey key;
    private final String issuer;
    private final Duration ttl;
    private final Clock clock;

    public JwtTokenService(AppProperties props, Clock clock) {
        String secret = props.jwt().secret();
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("Falta JWT_SECRET (clave base64 de al menos 32 bytes: openssl rand -base64 48)");
        }
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        this.issuer = props.jwt().issuer();
        this.ttl = Duration.ofHours(props.jwt().ttlHours());
        this.clock = clock;
    }

    @Override
    public String issue(User user) {
        Instant now = clock.instant();
        var builder = Jwts.builder()
                .subject(user.id())
                .issuer(issuer)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(ttl)))
                .claim("role", user.role().value())
                .claim("name", user.fullName());
        // La foto puede ser un data: URL de 2 MB; solo va en el token si es un enlace corto.
        String avatar = user.avatarUrl();
        if (avatar != null && avatar.startsWith("http") && avatar.length() <= MAX_AVATAR_IN_TOKEN) {
            builder.claim("avatar", avatar);
        }
        return builder.signWith(key, Jwts.SIG.HS256).compact();
    }

    @Override
    public Optional<CurrentUser> verify(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .requireIssuer(issuer)
                    .clock(() -> Date.from(clock.instant()))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            Role role = WireEnum.find(Role.class, claims.get("role", String.class)).orElse(Role.USER);
            return Optional.of(new CurrentUser(claims.getSubject(), role, claims.get("name", String.class)));
        } catch (JwtException | IllegalArgumentException ex) {
            return Optional.empty();
        }
    }
}
