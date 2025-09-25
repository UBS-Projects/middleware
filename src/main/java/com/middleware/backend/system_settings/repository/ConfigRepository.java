package com.middleware.backend.system_settings.repository;

import com.middleware.backend.system_settings.model.Config;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for persisting and querying DHIS2 settings.
 */
@Repository
public interface ConfigRepository extends JpaRepository<Config, String> {
    List<Config> findByModule(String module);

    List<Config> findByModuleAndKeyStartingWith(String module, String code);

    Config findByModuleAndKey(String module, String key);

    Optional<Config> findByKey(String key);

    @Query("SELECT DISTINCT c.module FROM Config c")
    List<String> findDistinctModules();

    Optional<Config> findByModuleAndId(String module, UUID id);
}