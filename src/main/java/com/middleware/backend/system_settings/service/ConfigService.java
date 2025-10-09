package com.middleware.backend.system_settings.service;

import com.middleware.backend.system_settings.dto.ConfigDto;
import com.middleware.backend.system_settings.mapper.ConfigMapper;
import com.middleware.backend.system_settings.model.Config;
import com.middleware.backend.system_settings.repository.ConfigRepository;
import com.middleware.backend.users.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;


/**
 * Service class for managing application configurations.
 * Handles CRUD operations on Config entities and provides module-specific configuration retrieval.
 */
@Service
public class ConfigService {

    @Autowired
    private ConfigRepository configRepository;

    /**
     * Saves a new configuration entry.
     * Checks if a config with the same key already exists; if so, returns null.
     * Sets auditing fields such as createdBy, updatedBy, createdAt, and updatedAt.
     *
     * @param config the ConfigDto object containing config details
     * @return the saved Config entity, or null if the key already exists
     */
    public Config saveConfig(ConfigDto config) {
        Optional<Config> exists = configRepository.findByKey(config.getKey());
        if (exists.isPresent()) return null;

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUser = authentication.getName();
        config.setCreatedAt(LocalDateTime.now());
        config.setUpdatedAt(LocalDateTime.now());
        config.setCreatedBy(currentUser);
        config.setUpdatedBy(currentUser);
        return configRepository.save(ConfigMapper.toEntity(config));
    }

    /**
     * Updates the value of an existing configuration identified by its key.
     * Updates auditing fields updatedBy and updatedAt.
     *
     * @param key   the config key to update
     * @param value the new value for the config
     * @return the updated ConfigDto, or null if the config does not exist
     */
    public ConfigDto updateConfig(String key, String value) {
        Optional<Config> config = configRepository.findByKey(key);
        if (config != null) {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String currentUser = authentication.getName();
            config.get().setValue(value);
            config.get().setUpdatedBy(currentUser);
            config.get().setUpdatedAt(LocalDateTime.now());
            return ConfigMapper.toDTO(configRepository.save(config.get()));
        }
        return null;
    }

    /**
     * Retrieves all configurations matching the given specification and pagination.
     *
     * @param spec     the Specification for filtering configs
     * @param pageable pagination and sorting information
     * @return a page of ConfigDto objects
     */
    public Page<?> findAll(Specification<Config> spec, Pageable pageable) {
        Page<ConfigDto> page = configRepository.findAll(spec, pageable)
                .map(ConfigMapper::toDTO);
        return page;
    }

    /**
     * Retrieves detailed information for a configuration by key.
     *
     * @param key the config key
     * @return the corresponding ConfigDto
     * @throws NoSuchElementException if the config does not exist
     */
    public ConfigDto getConfigDetails(String key) {
        return configRepository.findByKey(key).map(ConfigMapper::toDTO).get();
    }

    /**
     * Retrieves a module configuration as a key-value map for a given code.
     * If the config key contains a dot ('.'), returns the part after the first dot as the map key.
     *
     * @param code the config key to retrieve
     * @return a map containing the config key and its value
     * @throws RuntimeException if the config does not exist
     */
    public Map<String, String> getModuleConfig(String code) {
        Optional<Config> config = configRepository.findByKey(code);
        if (config.isEmpty()) {
            throw new RuntimeException("Config not found for code: " + code);
        }

        String fullKey = config.get().getKey();

        // Extract part after the first dot
        int dotIndex = fullKey.indexOf('.');
        String key = (dotIndex != -1 && dotIndex < fullKey.length() - 1)
                ? fullKey.substring(dotIndex + 1)
                : fullKey;

        Map<String, String> map = new HashMap<>();
        map.put(key, config.get().getValue());

        return map;
    }
}
