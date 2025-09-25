package com.middleware.backend.kaotocamel.service;

import com.middleware.backend.kaotocamel.dto.*;
import com.middleware.backend.kaotocamel.model.*;
import com.middleware.backend.kaotocamel.repository.*;
import com.middleware.backend.kaotocamel.spec.IntegrationMappingSpecification;
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
 * Service for managing IntegrationMapping entities
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class IntegrationMappingService {

    private final IntegrationMappingRepository mappingRepository;
    private final IntegratedApiRepository apiRepository;

    @Transactional
    public IntegrationMappingDto create(IntegrationMappingRequestDto request) {
        log.info("Creating integration mapping for API: {}", request.getMiddlewareApiName());

        // Validate integrated API exists
        IntegratedApi api = apiRepository.findById(request.getIntegratedApiId())
                .orElseThrow(() -> new RuntimeException("Integrated API not found: " + request.getIntegratedApiId()));

        // Check for duplicate external key
        if (mappingRepository.existsByMiddlewareApiNameAndExternalKeyAndIsActiveTrue(
                request.getMiddlewareApiName(), request.getExternalKey())) {
            throw new IllegalArgumentException("External key '" + request.getExternalKey() +
                    "' already exists for API: " + request.getMiddlewareApiName());
        }

        IntegrationMapping entity = new IntegrationMapping();
        mapRequestToEntity(request, entity, api);

        IntegrationMapping saved = mappingRepository.save(entity);
        log.info("Created integration mapping: {} -> {}",
                saved.getMiddlewareApiName(), saved.getExternalKey());

        return mapEntityToDto(saved);
    }

    @Transactional
    public IntegrationMappingDto update(Long id, IntegrationMappingRequestDto request) {
        log.info("Updating integration mapping: {}", id);

        IntegrationMapping entity = mappingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Integration mapping not found: " + id));

        // Validate integrated API if changed
        IntegratedApi api = entity.getIntegratedApi();
        if (!entity.getIntegratedApiId().equals(request.getIntegratedApiId())) {
            api = apiRepository.findById(request.getIntegratedApiId())
                    .orElseThrow(() -> new RuntimeException("Integrated API not found: " + request.getIntegratedApiId()));
        }

        mapRequestToEntity(request, entity, api);
        IntegrationMapping saved = mappingRepository.save(entity);

        log.info("Updated integration mapping: {}", saved.getId());
        return mapEntityToDto(saved);
    }

    @Transactional
    public void softDelete(Long id) {
        log.info("Soft deleting integration mapping: {}", id);

        IntegrationMapping entity = mappingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Integration mapping not found: " + id));

        entity.setIsActive(false);
        mappingRepository.save(entity);

        log.info("Soft deleted integration mapping: {}", id);
    }

    @Transactional(readOnly = true)
    public Optional<IntegrationMappingDto> findById(Long id) {
        return mappingRepository.findById(id)
                .map(this::mapEntityToDto);
    }

    @Transactional(readOnly = true)
    public Page<IntegrationMappingDto> findWithFilters(String middlewareApiName, Long integratedApiId,
                                                       String mappingType, String externalKey,
                                                       Boolean isActive, Pageable pageable) {

        Specification<IntegrationMapping> spec = Specification.where(null);

        if (middlewareApiName != null && !middlewareApiName.isEmpty()) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("middlewareApiName"), middlewareApiName));
        }

        if (integratedApiId != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("integratedApiId"), integratedApiId));
        }

        if (mappingType != null && !mappingType.isEmpty()) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("mappingType"), IntegrationMapping.MappingType.valueOf(mappingType)));
        }

        if (externalKey != null && !externalKey.isEmpty()) {
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("externalKey")), "%" + externalKey.toLowerCase() + "%"));
        }

        if (isActive != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("isActive"), isActive));
        }

        return mappingRepository.findAll(spec, pageable)
                .map(this::mapEntityToDto);
    }

    @Transactional(readOnly = true)
    public List<IntegrationMappingDto> findByMiddlewareApi(String middlewareApiName) {
        return mappingRepository.findByMiddlewareApiNameAndIsActiveTrue(middlewareApiName)
                .stream()
                .map(this::mapEntityToDto)
                .collect(Collectors.toList());
    }
    // أضف هذا الـ method للـ service
    @Transactional(readOnly = true)
    public Page<IntegrationMappingDto> findWithAdvancedFilters(
            String middlewareApiName, Long integratedApiId, String integratedApiCode,
            String mappingType, String data, String externalKey, String attribute,
            Boolean isActive, String notes,
            LocalDateTime createdAfter, LocalDateTime createdBefore,
            LocalDateTime updatedAfter, LocalDateTime updatedBefore,
            String search, Long minId, Long maxId,
            Pageable pageable) {

        log.debug("Searching with advanced filters - middlewareApi: {}, integratedApiId: {}, mappingType: {}",
                middlewareApiName, integratedApiId, mappingType);

        Specification<IntegrationMapping> spec = IntegrationMappingSpecification.buildSpecification(
                middlewareApiName, integratedApiId, integratedApiCode, mappingType,
                data, externalKey, attribute, isActive, notes,
                createdAfter, createdBefore, updatedAfter, updatedBefore,
                search, minId, maxId
        );

        Page<IntegrationMappingDto> result = mappingRepository.findAll(spec, pageable)
                .map(this::mapEntityToDto);

        log.debug("Found {} results out of {} total", result.getNumberOfElements(), result.getTotalElements());
        return result;
    }
    @Transactional(readOnly = true)
    public List<IntegrationMappingDto> findAll() {
        return mappingRepository.findAll()
                .stream()
                .map(this::mapEntityToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<String> getDistinctMiddlewareApis() {
        return mappingRepository.findDistinctMiddlewareApiNames();
    }

    public Map<String, Object> validateMapping(IntegrationMappingRequestDto request) {
        Map<String, Object> validation = new HashMap<>();
        List<String> errors = new ArrayList<>();

        // Validate mapping type and data format
        try {
            IntegrationMapping.MappingType type = IntegrationMapping.MappingType.valueOf(request.getMappingType());

            switch (type) {
                case DATA_ELEMENT_WITH_DISAGGREGATION:
                    if (!request.getData().contains(".")) {
                        errors.add("DATA_ELEMENT_WITH_DISAGGREGATION requires data in format DE_UID.COC_UID");
                    }
                    break;
                case DATA_ELEMENT:
                    if (request.getData().contains(".")) {
                        errors.add("DATA_ELEMENT should not contain disaggregation (no dots)");
                    }
                    break;
            }
        } catch (IllegalArgumentException e) {
            errors.add("Invalid mapping type: " + request.getMappingType());
        }

        // Check if API exists
        if (!apiRepository.existsById(request.getIntegratedApiId())) {
            errors.add("Integrated API not found: " + request.getIntegratedApiId());
        }

        // Check for duplicate external key
        if (mappingRepository.existsByMiddlewareApiNameAndExternalKeyAndIsActiveTrue(
                request.getMiddlewareApiName(), request.getExternalKey())) {
            errors.add("External key already exists: " + request.getExternalKey());
        }

        validation.put("valid", errors.isEmpty());
        validation.put("errors", errors);

        return validation;
    }

    @Transactional
    public List<IntegrationMappingDto> createBatch(List<IntegrationMappingRequestDto> requests) {
        List<IntegrationMappingDto> created = new ArrayList<>();

        for (IntegrationMappingRequestDto request : requests) {
            try {
                created.add(create(request));
            } catch (Exception e) {
                log.error("Failed to create mapping: {}", e.getMessage());
                throw new RuntimeException("Batch creation failed at item " + created.size() + ": " + e.getMessage());
            }
        }

        return created;
    }

    @Transactional
    public Map<String, Object> importMappings(List<IntegrationMappingRequestDto> mappings) {
        Map<String, Object> result = new HashMap<>();
        List<IntegrationMappingDto> imported = new ArrayList<>();
        List<Map<String, Object>> errors = new ArrayList<>();

        for (int i = 0; i < mappings.size(); i++) {
            IntegrationMappingRequestDto request = mappings.get(i);
            try {
                imported.add(create(request));
            } catch (Exception e) {
                Map<String, Object> error = new HashMap<>();
                error.put("index", i);
                error.put("externalKey", request.getExternalKey());
                error.put("error", e.getMessage());
                errors.add(error);
            }
        }

        result.put("success", errors.isEmpty());
        result.put("imported", imported.size());
        result.put("errors", errors);

        return result;
    }

    private void mapRequestToEntity(IntegrationMappingRequestDto request, IntegrationMapping entity, IntegratedApi api) {
        entity.setMiddlewareApiName(request.getMiddlewareApiName());
        entity.setIntegratedApi(api);
        entity.setMappingType(IntegrationMapping.MappingType.valueOf(request.getMappingType()));
        entity.setData(request.getData());
        entity.setAttribute(request.getAttribute());
        entity.setExternalKey(request.getExternalKey());
        entity.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);
        entity.setNotes(request.getNotes());
    }

    private IntegrationMappingDto mapEntityToDto(IntegrationMapping entity) {
        return IntegrationMappingDto.builder()
                .id(entity.getId())
                .middlewareApiName(entity.getMiddlewareApiName())
                .integratedApiId(entity.getIntegratedApiId())
                .integratedApiCode(entity.getIntegratedApi() != null ? entity.getIntegratedApi().getCode() : null)
                .integratedApiName(entity.getIntegratedApi() != null ? entity.getIntegratedApi().getName() : null)
                .mappingType(entity.getMappingType().toString())
                .data(entity.getData())
                .attribute(entity.getAttribute())
                .externalKey(entity.getExternalKey())
                .isActive(entity.getIsActive())
                .notes(entity.getNotes())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}