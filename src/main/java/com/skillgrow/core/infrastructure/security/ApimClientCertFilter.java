package com.skillgrow.core.infrastructure.security;

import com.skillgrow.core.infrastructure.config.AppProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * mTLS APIM → App Service. App Service (clientCertMode=Required) exige el certificado y lo reenvía en
 * X-ARR-ClientCert, pero NO lo valida: aquí se compara su huella SHA-1 con APIM_CERT_THUMBPRINT.
 * Si no coincide → 403. Se apaga en local con APIM_CERT_CHECK=false.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class ApimClientCertFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-ARR-ClientCert";
    private static final Logger log = LoggerFactory.getLogger(ApimClientCertFilter.class);

    private final boolean enabled;
    private final String expectedThumbprint;
    private final ErrorWriter errors;

    public ApimClientCertFilter(AppProperties props, ErrorWriter errors) {
        this.enabled = props.apim().certCheck();
        this.expectedThumbprint = normalize(props.apim().certThumbprint());
        this.errors = errors;
        if (enabled && expectedThumbprint.isEmpty()) {
            throw new IllegalStateException("APIM_CERT_CHECK=true pero falta APIM_CERT_THUMBPRINT");
        }
    }

    /** El health check interno de App Service no pasa por APIM. */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest req) {
        return !enabled || "/health".equals(req.getRequestURI());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String header = req.getHeader(HEADER);
        if (header == null || !expectedThumbprint.equals(thumbprintOf(header))) {
            log.warn("Llamada rechazada: certificado de cliente ausente o distinto al de APIM ({})", req.getRequestURI());
            errors.write(req, res, 403, "FORBIDDEN", "Solo se aceptan llamadas a través de API Management.");
            return;
        }
        chain.doFilter(req, res);
    }

    /** SHA-1 del certificado DER que viene en base64, en hexadecimal mayúscula. "" si no se puede leer. */
    static String thumbprintOf(String base64Cert) {
        try {
            byte[] der = Base64.getMimeDecoder().decode(base64Cert);
            X509Certificate cert = (X509Certificate) CertificateFactory.getInstance("X.509")
                    .generateCertificate(new ByteArrayInputStream(der));
            byte[] sha1 = MessageDigest.getInstance("SHA-1").digest(cert.getEncoded());
            return HexFormat.of().withUpperCase().formatHex(sha1);
        } catch (Exception ex) {
            return "";
        }
    }

    /** Acepta la huella con ":" o espacios, como la imprime openssl. */
    static String normalize(String thumbprint) {
        return thumbprint == null ? "" : thumbprint.replaceAll("[:\\s]", "").toUpperCase(Locale.ROOT);
    }
}
