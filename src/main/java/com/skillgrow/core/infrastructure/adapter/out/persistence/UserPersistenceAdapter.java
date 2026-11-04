package com.skillgrow.core.infrastructure.adapter.out.persistence;

import com.skillgrow.core.domain.model.AccountStatus;
import com.skillgrow.core.domain.model.PageQuery;
import com.skillgrow.core.domain.model.PageResult;
import com.skillgrow.core.domain.model.Role;
import com.skillgrow.core.domain.model.User;
import com.skillgrow.core.domain.model.UserSummary;
import com.skillgrow.core.domain.port.out.UserRepositoryPort;
import com.skillgrow.core.infrastructure.adapter.out.persistence.entity.UserEntity;
import com.skillgrow.core.infrastructure.adapter.out.persistence.repository.UserJpaRepository;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

/** Implementa UserRepositoryPort sobre Azure SQL. Si se cambia la base, solo cambia este paquete. */
@Repository
class UserPersistenceAdapter implements UserRepositoryPort {

    private final UserJpaRepository repo;
    private final UserPersistenceMapper mapper;

    UserPersistenceAdapter(UserJpaRepository repo, UserPersistenceMapper mapper) {
        this.repo = repo;
        this.mapper = mapper;
    }

    @Override
    public Optional<User> findById(String id) {
        return repo.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<User> findByEmail(String normalizedEmail) {
        return repo.findByEmail(normalizedEmail).map(mapper::toDomain);
    }

    @Override
    public Optional<User> findByGoogleSub(String googleSub) {
        return repo.findByGoogleSub(googleSub).map(mapper::toDomain);
    }

    @Override
    public boolean existsByEmail(String normalizedEmail) {
        return repo.existsByEmail(normalizedEmail);
    }

    @Override
    public User save(User user) {
        UserEntity entity = repo.findById(user.id()).orElseGet(UserEntity::new);
        mapper.copyInto(user, entity);
        return mapper.toDomain(repo.save(entity));
    }

    @Override
    public Map<String, UserSummary> findSummaries(Collection<String> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        return repo.findSummaries(ids.stream().distinct().toList()).stream()
                .map(v -> new UserSummary(v.getId(), v.getFullName(), v.getAvatarUrl()))
                .collect(Collectors.toMap(UserSummary::id, Function.identity()));
    }

    @Override
    public PageResult<User> search(String search, Role role, AccountStatus status, String sortField,
                                   boolean ascending, PageQuery page) {
        Specification<UserEntity> spec = (root, query, cb) -> {
            List<Predicate> and = new ArrayList<>();
            if (role != null) {
                and.add(cb.equal(root.get("role"), role.value()));
            }
            if (status != null) {
                and.add(cb.equal(root.get("status"), status.value()));
            }
            if (search != null && !search.isBlank()) {
                String like = "%" + search.trim() + "%";
                and.add(cb.or(cb.like(root.get("fullName"), like), cb.like(root.get("email"), like)));
            }
            return cb.and(and.toArray(Predicate[]::new));
        };
        String property = "fullName".equals(sortField) ? "fullName" : "createdAtEpochMs";
        Sort sort = Sort.by(ascending ? Sort.Direction.ASC : Sort.Direction.DESC, property);
        return Pages.toResult(repo.findAll(spec, Pages.of(page, sort)), page, mapper::toDomain);
    }

    @Override
    public long count() {
        return repo.count();
    }

    @Override
    public long countByStatus(AccountStatus status) {
        return repo.countByStatus(status.value());
    }

    @Override
    public long countMentors() {
        return repo.countMentors();
    }

    @Override
    public List<Long> createdSince(long fromEpochMs) {
        return repo.createdSince(fromEpochMs);
    }
}
