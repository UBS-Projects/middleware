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
 * Service for retrieving and persisting DHIS2 system settings.
 */
@Service
public class ConfigService {

    @Autowired
    private ConfigRepository configRepository;




    public Config saveConfig(ConfigDto config) {
        Optional<Config> exists = configRepository.findByKey(config.getKey());
        if (exists.isPresent())return null;
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUser = authentication.getName();
        config.setCreatedAt(LocalDateTime.now());
        config.setUpdatedAt(LocalDateTime.now());
        config.setCreatedBy(currentUser);
        config.setUpdatedBy(currentUser);
        return configRepository.save(ConfigMapper.toEntity(config));
    }

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

    public Page<?> findAll(Specification<Config> spec, Pageable pageable) {
        Page<ConfigDto> page = configRepository.findAll(spec, pageable).map(
                ConfigMapper::toDTO
        );
        return page;
    }

    public ConfigDto getConfigDetails(String key) {
        return configRepository.findByKey(key).map(ConfigMapper::toDTO).get();
    }


    public Map<String, String> getModuleConfig(String code) {
        System.out.println("Mohammad kadoumi");

        Optional<Config> config = configRepository.findByKey(code);
        if (config.isEmpty()) {
            throw new RuntimeException("Config not found for code: " + code);
        }

        String fullKey = config.get().getKey();
        System.out.println(fullKey);

        // Extract part after the first dot
        int dotIndex = fullKey.indexOf('.');
        String key = (dotIndex != -1 && dotIndex < fullKey.length() - 1)
                ? fullKey.substring(dotIndex + 1)
                : fullKey;

        Map<String, String> map = new HashMap<>();
        map.put(key, config.get().getValue());

        System.out.println("**************&&&&&&&&&&&&&&&");
        System.out.println(key);

        return map;
    }
}