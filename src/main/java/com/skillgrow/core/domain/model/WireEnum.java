package com.skillgrow.core.domain.model;

import com.skillgrow.core.domain.exception.ValidationException;
import java.util.Arrays;
import java.util.Optional;

/** Enum con el valor exacto que viaja en el JSON del contrato ("pendiente", "google_meet"…). */
public interface WireEnum {

    String value();

    static <E extends Enum<E> & WireEnum> Optional<E> find(Class<E> type, String value) {
        if (value == null) {
            return Optional.empty();
        }
        return Arrays.stream(type.getEnumConstants()).filter(e -> e.value().equals(value)).findFirst();
    }

    /** Convierte el valor del JSON o lanza VALIDATION_ERROR sobre el campo indicado. */
    static <E extends Enum<E> & WireEnum> E parse(Class<E> type, String value, String field, String message) {
        return find(type, value).orElseThrow(() -> ValidationException.of(field, message));
    }
}
