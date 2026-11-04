package com.skillgrow.core.domain.model;

import java.util.Map;

/** Una sesión con los resúmenes de sus participantes, para armar la respuesta sin más consultas. */
public record SessionDetails(Session session, Map<String, UserSummary> people) {

    public UserSummary person(String id) {
        UserSummary s = people.get(id);
        return s != null ? s : UserSummary.deleted(id);
    }
}
