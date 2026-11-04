package com.skillgrow.core.domain.model;

import com.skillgrow.core.domain.exception.FieldIssue;
import com.skillgrow.core.domain.exception.ValidationException;
import java.util.ArrayList;
import java.util.List;

/** Lugar de una sesión presencial. */
public record Location(String name, String address, String notes) {

    public static Location validated(String name, String address, String notes) {
        List<FieldIssue> issues = new ArrayList<>();
        if (name == null || name.isBlank()) {
            issues.add(new FieldIssue("location.name", "Indica el nombre del lugar."));
        }
        if (address == null || address.isBlank()) {
            issues.add(new FieldIssue("location.address", "Indica la dirección o referencia."));
        }
        ValidationException.throwIfAny(issues);
        String cleanNotes = notes == null || notes.isBlank() ? null : notes.trim();
        return new Location(name.trim(), address.trim(), cleanNotes);
    }
}
