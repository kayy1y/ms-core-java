package com.skillgrow.core.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skillgrow.core.infrastructure.adapter.in.rest.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Clock;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

/** Los filtros responden antes de llegar a los controllers; usan el mismo formato de error. */
@Component
public class ErrorWriter {

    private final ObjectMapper json;
    private final Clock clock;

    public ErrorWriter(ObjectMapper json, Clock clock) {
        this.json = json;
        this.clock = clock;
    }

    public void write(HttpServletRequest req, HttpServletResponse res, int status, String code, String message)
            throws IOException {
        res.setStatus(status);
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        res.setCharacterEncoding("UTF-8");
        json.writeValue(res.getOutputStream(),
                new ErrorResponse(status, code, message, List.of(), clock.millis(), req.getRequestURI()));
    }
}
