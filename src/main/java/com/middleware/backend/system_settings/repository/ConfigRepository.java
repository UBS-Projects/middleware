package com.middleware.backend.system_settings.repository;

import com.middleware.backend.system_settings.model.Config;
import com.middleware.backend.users.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for persisting and querying DHIS2 settings.
 */
@Repository
public interface ConfigRepository extends JpaRepository<Config, String> {
    Optional<Config> findByKey(String key);
    Page<Config> findAll(Specification<Config> spec, Pageable pageable);
    List<Config> findByKeyStartingWith(String s);
}