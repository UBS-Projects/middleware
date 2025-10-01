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

    public List<String> getModules() {
        return configRepository.findDistinctModules();
    }

    public Map<String, String> getModuleConfig(String module, String codePrefix) {
        List<Config> configs;
        if (codePrefix != null) {
            configs = configRepository.findByModuleAndKeyStartingWith(module, codePrefix + ".");
        } else {
            configs = configRepository.findByModule(module);
        }

        Map<String, String> map = new HashMap<>();
        for (Config c : configs) {
            String key = c.getKey();
            if (codePrefix != null) {
                key = key.substring((codePrefix + ".").length());
            }
            map.put(key, c.getValue());
        }
        return map;
    }

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

    public ConfigDto updateConfig(String module, String key, String value) {
        Config config = configRepository.findByModuleAndKey(module, key);
        if (config != null) {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String currentUser = authentication.getName();
            config.setValue(value);
            config.setUpdatedBy(currentUser);
            config.setUpdatedAt(LocalDateTime.now());
            return ConfigMapper.toDTO(configRepository.save(config));
        }
        return null;
    }

    public Page<?> findAll(Specification<Config> spec, Pageable pageable) {
        Page<ConfigDto> page = configRepository.findAll(spec, pageable).map(
                ConfigMapper::toDTO
        );
        return page;
    }

    public ConfigDto getModuleSpecificConfig(String module, UUID id) {
        return configRepository.findByModuleAndId(module,id).map(ConfigMapper::toDTO).get();
    }


    public Map<String, String> getModuleConfig(String codePrefix) {
        List<Config> configs;
        configs = configRepository.findByKeyStartingWith(codePrefix + ".");


        Map<String, String> map = new HashMap<>();
        for (Config c : configs) {
            String key = c.getKey();
            if (codePrefix != null) {
                key = key.substring((codePrefix + ".").length());
            }
            map.put(key, c.getValue());
        }
        return map;
    }
}