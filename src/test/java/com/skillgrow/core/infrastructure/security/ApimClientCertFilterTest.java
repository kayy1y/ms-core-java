package com.skillgrow.core.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skillgrow.core.TestData;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/** El backend solo acepta llamadas que traen el certificado de cliente de APIM. */
class ApimClientCertFilterTest {

    private static String resource(String name) throws IOException {
        return new ClassPathResource(name).getContentAsString(StandardCharsets.UTF_8).trim();
    }

    private ApimClientCertFilter filter(String thumbprint) {
        return new ApimClientCertFilter(TestData.props(true, thumbprint), new ErrorWriter(new ObjectMapper(), Clock.systemUTC()));
    }

    @Test
    void calculaLaHuellaComoOpenssl() throws IOException {
        String expected = ApimClientCertFilter.normalize(resource("apim-test-cert.sha1"));

        assertThat(ApimClientCertFilter.thumbprintOf(resource("apim-test-cert.b64"))).isEqualTo(expected);
        assertThat(ApimClientCertFilter.thumbprintOf("no-es-un-certificado")).isEmpty();
    }

    @Test
    void conElCertificadoCorrectoPasa() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/v1/me");
        req.addHeader(ApimClientCertFilter.HEADER, resource("apim-test-cert.b64"));
        MockHttpServletResponse res = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter(resource("apim-test-cert.sha1")).doFilter(req, res, chain);

        assertThat(chain.getRequest()).isNotNull();
        assertThat(res.getStatus()).isEqualTo(200);
    }

    @Test
    void sinCertificadoOConOtroDa403() throws Exception {
        MockHttpServletResponse res = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter("00:11:22").doFilter(new MockHttpServletRequest("GET", "/v1/me"), res, chain);

        assertThat(res.getStatus()).isEqualTo(403);
        assertThat(chain.getRequest()).isNull();
        assertThat(res.getContentAsString()).contains("\"code\":\"FORBIDDEN\"");
    }

    @Test
    void elHealthCheckDeAppServiceNoNecesitaCertificado() throws Exception {
        MockHttpServletResponse res = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter("00:11:22").doFilter(new MockHttpServletRequest("GET", "/health"), res, chain);

        assertThat(chain.getRequest()).isNotNull();
    }
}
