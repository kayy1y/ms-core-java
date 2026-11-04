package com.skillgrow.core.domain.port.out;

import com.skillgrow.core.domain.model.AccountStatus;
import com.skillgrow.core.domain.model.PageQuery;
import com.skillgrow.core.domain.model.PageResult;
import com.skillgrow.core.domain.model.Role;
import com.skillgrow.core.domain.model.User;
import com.skillgrow.core.domain.model.UserSummary;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Lo que el dominio necesita para guardar y leer usuarios. Lo implementa el adaptador de Azure SQL. */
public interface UserRepositoryPort {

    Optional<User> findById(String id);

    Optional<User> findByEmail(String normalizedEmail);

    Optional<User> findByGoogleSub(String googleSub);

    boolean existsByEmail(String normalizedEmail);

    User save(User user);

    Map<String, UserSummary> findSummaries(Collection<String> ids);

    /** sort: createdAt | fullName, con asc/desc. */
    PageResult<User> search(String search, Role role, AccountStatus status, String sortField,
                            boolean ascending, PageQuery page);

    long count();

    long countByStatus(AccountStatus status);

    /** Usuarios con al menos una habilidad para enseñar. */
    long countMentors();

    /** createdAtEpochMs de los usuarios creados desde ese instante (para la actividad de 30 días). */
    List<Long> createdSince(long fromEpochMs);
}
