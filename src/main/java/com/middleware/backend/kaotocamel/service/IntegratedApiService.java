package com.middleware.backend.kaotocamel.service;

import com.middleware.backend.kaotocamel.dto.AnalyticsResponseDto;
import com.middleware.backend.kaotocamel.dto.IntegratedApiDto;
import com.middleware.backend.kaotocamel.dto.IntegratedApiRequestDto;
import com.middleware.backend.kaotocamel.model.IntegratedApi;
import com.middleware.backend.kaotocamel.repository.IntegratedApiRepository;
import com.middleware.backend.kaotocamel.spec.IntegratedApiSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Enhanced Service for managing IntegratedApi entities with dynamic DHIS2 code support
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class IntegratedApiService {

    private final IntegratedApiRepository repository;
    private final Dhis2ClientService dhis2Client;

    @Transactional
    public IntegratedApiDto create(IntegratedApiRequestDto request) {
        log.info("Creating integrated API with code: {}", request.getCode());

        if (repository.existsByCode(request.getCode())) {
            throw new IllegalArgumentException("API with code '" + request.getCode() + "' already exists");
        }

        IntegratedApi entity = new IntegratedApi();
        mapRequestToEntity(request, entity);

        IntegratedApi saved = repository.save(entity);
        log.info("Created integrated API: {} (ID: {})", saved.getCode(), saved.getId());

        return mapEntityToDto(saved);
    }

    @Transactional
    public IntegratedApiDto update(Long id, IntegratedApiRequestDto request) {
        log.info("Updating integrated API: {}", id);

        IntegratedApi entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Integrated API not found: " + id));

        if (!entity.getCode().equals(request.getCode()) &&
                repository.existsByCode(request.getCode())) {
            throw new IllegalArgumentException("API with code '" + request.getCode() + "' already exists");
        }

        mapRequestToEntity(request, entity);
        IntegratedApi saved = repository.save(entity);

        log.info("Updated integrated API: {}", saved.getCode());
        return mapEntityToDto(saved);
    }

    @Transactional
    public void softDelete(Long id) {
        log.info("Soft deleting integrated API: {}", id);

        IntegratedApi entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Integrated API not found: " + id));

        entity.setIsActive(false);
        repository.save(entity);

        log.info("Soft deleted integrated API: {}", entity.getCode());
    }

    @Transactional(readOnly = true)
    public Optional<IntegratedApiDto> findById(Long id) {
        return repository.findById(id)
                .map(this::mapEntityToDto);
    }

    @Transactional(readOnly = true)
    public Optional<IntegratedApiDto> findByCode(String code) {
        return repository.findByCode(code)
                .map(this::mapEntityToDto);
    }

    @Transactional(readOnly = true)
    public Page<IntegratedApiDto> findWithFilters(String code, String name, String type,
                                                  String integratedSystem, Boolean isActive,
                                                  Pageable pageable) {
        return findWithAdvancedFilters(code, name, null, type, integratedSystem, isActive,
                null, null, null, null, null, null, null, null, pageable);
    }

    @Transactional(readOnly = true)
    public Page<IntegratedApiDto> findWithAdvancedFilters(
            String code, String name, String apiUrl, String type,
            String integratedSystem, Boolean isActive, String description,
            LocalDateTime createdAfter, LocalDateTime createdBefore,
            LocalDateTime updatedAfter, LocalDateTime updatedBefore,
            String search, Long minId, Long maxId,
            Pageable pageable) {

        Specification<IntegratedApi> spec = IntegratedApiSpecification.buildSpecification(
                code, name, apiUrl, type, integratedSystem, isActive, description,
                createdAfter, createdBefore, updatedAfter, updatedBefore,
                search, minId, maxId
        );

        return repository.findAll(spec, pageable)
                .map(this::mapEntityToDto);
    }

    @Transactional(readOnly = true)
    public List<String> getDistinctIntegratedSystems() {
        return repository.findAll().stream()
                .map(IntegratedApi::getIntegratedSystem)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    /**
     * Dynamic DHIS2 connection test supporting _dhis2Code
     */
    public Map<String, Object> testApiConnection(Long id, String period, String dhis2Code) {
        log.info("Testing API connection for ID: {} with period: {} and DHIS2 code: {}", id, period, dhis2Code);

        IntegratedApi api = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Integrated API not found: " + id));

        Map<String, Object> result = new HashMap<>();

        try {
            Map<String, Object> response = dhis2Client.executeApiCall(api, period, dhis2Code);

            result.put("success", true);
            result.put("message", "Connection successful");
            result.put("responseType", response.get("type"));

            if ("ANALYTICS".equals(response.get("type"))) {
                AnalyticsResponseDto analytics = (AnalyticsResponseDto) response.get("data");
                result.put("rowCount", analytics.getRows().size());
            }

        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "Connection failed");
            result.put("error", e.getMessage());
        }

        return result;
    }

    private void mapRequestToEntity(IntegratedApiRequestDto request, IntegratedApi entity) {
        entity.setCode(request.getCode());
        entity.setName(request.getName());
        entity.setApiUrl(request.getApiUrl());
        entity.setType(IntegratedApi.ApiType.valueOf(request.getType()));
        entity.setIntegratedSystem(request.getIntegratedSystem());
        entity.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);
        entity.setDescription(request.getDescription());
    }

    private IntegratedApiDto mapEntityToDto(IntegratedApi entity) {
        return IntegratedApiDto.builder()
                .id(entity.getId())
                .code(entity.getCode())
                .name(entity.getName())
                .apiUrl(entity.getApiUrl())
                .type(entity.getType().toString())
                .integratedSystem(entity.getIntegratedSystem())
                .isActive(entity.getIsActive())
                .description(entity.getDescription())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
