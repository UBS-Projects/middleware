package com.middleware.backend.camel.service;

import com.middleware.backend.camel.dto.IntegratedApiDto;
import com.middleware.backend.camel.dto.IntegratedApiRequestDto;
import com.middleware.backend.camel.model.IntegratedApi;
import com.middleware.backend.camel.model.IntegrationMapping;
import com.middleware.backend.camel.repository.IntegrationMappingRepository;
import com.middleware.backend.camel.repository.IntegratedApiRepository;
import com.middleware.backend.camel.spec.IntegratedApiSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
    private final IntegrationMappingRepository mappingRepository;
    private final Dhis2ClientService dhis2Client;

    @Transactional
    public IntegratedApiDto create(IntegratedApiRequestDto request) {
        log.info("Creating integrated API with code: {}", request.getCode());

        if (repository.existsByCode(request.getCode())) {
            throw new IllegalArgumentException("API with code '" + request.getCode() + "' already exists");
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUser = authentication.getName();

        IntegratedApi entity = new IntegratedApi();
        mapRequestToEntity(request, entity);

        entity.setCreatedBy(currentUser);
        entity.setUpdatedBy(currentUser);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());

        IntegratedApi saved = repository.save(entity);
        log.info("Created integrated API: {} (ID: {}) by user: {}", saved.getCode(), saved.getId(), currentUser);

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

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUser = authentication.getName();

        mapRequestToEntity(request, entity);


        entity.setUpdatedBy(currentUser);
        entity.setUpdatedAt(LocalDateTime.now());

        IntegratedApi saved = repository.save(entity);

        log.info("Updated integrated API: {} by user: {}", saved.getCode(), currentUser);
        return mapEntityToDto(saved);
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
    public Page<IntegratedApiDto> findWithAdvancedFilters(
            String code,
            String name,
            String apiUrl,
            String boundApiCode,
            String type,
            String integratedSystem,
            Boolean isActive,
            String description,
            LocalDateTime createdAfter,
            LocalDateTime createdBefore,
            LocalDateTime updatedAfter,
            LocalDateTime updatedBefore,
            String search,
            Long minId,
            Long maxId,
            Pageable pageable) {

        Specification<IntegratedApi> spec = IntegratedApiSpecification.buildSpecification(
                code, name, apiUrl, boundApiCode, type, integratedSystem, isActive, description,
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
     * Toggle active status of an integrated API (activate/deactivate)
     *
     * @param id The API ID
     * @param force if true, force-deactivate linked mappings before deactivating the API
     * @return Updated API DTO
     */
    @Transactional
    public IntegratedApiDto toggleActiveStatus(Long id, boolean force) {
        log.info("Toggling active status for integrated API: {} (force={})", id, force);

        IntegratedApi entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Integrated API not found: " + id));

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUser = authentication.getName();

        boolean newStatus = !entity.getIsActive();

        if (!newStatus) {
            long activeMappings = mappingRepository.countByIntegratedApiIdAndIsActiveTrue(entity.getId());
            if (activeMappings > 0) {
                if (!force) {
                    String msg = String.format("Cannot deactivate IntegratedApi id=%d (code=%s): there are %d active mapping(s) linked. Use force=true to deactivate them.",
                            entity.getId(), entity.getCode(), activeMappings);
                    log.warn(msg);
                    throw new IllegalStateException(msg);
                } else {
                    deactivateAssociatedMappings(entity.getId());
                }
            }
        }

        entity.setIsActive(newStatus);


        entity.setUpdatedBy(currentUser);
        entity.setUpdatedAt(LocalDateTime.now());

        IntegratedApi saved = repository.save(entity);

        log.info("Toggled integrated API {} status to: {} by user: {}",
                id, newStatus ? "ACTIVE" : "INACTIVE", currentUser);

        return mapEntityToDto(saved);
    }

    /**
     * Activate an integrated API
     *
     * @param id The API ID
     * @return Updated API DTO
     */
    @Transactional
    public IntegratedApiDto activate(Long id) {
        log.info("Activating integrated API: {}", id);

        IntegratedApi entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Integrated API not found: " + id));

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUser = authentication.getName();

        entity.setIsActive(true);

        entity.setUpdatedBy(currentUser);
        entity.setUpdatedAt(LocalDateTime.now());

        IntegratedApi saved = repository.save(entity);

        log.info("Activated integrated API: {} by user: {}", id, currentUser);

        return mapEntityToDto(saved);
    }
    /**
     * Deactivate an integrated API (alternative to soft delete)
     *
     * @param id The API ID
     * @return Updated API DTO
     */
    @Transactional
    public IntegratedApiDto deactivate(Long id) {
        log.info("Deactivating integrated API: {}", id);

        IntegratedApi entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Integrated API not found: " + id));

        long activeMappings = mappingRepository.countByIntegratedApiIdAndIsActiveTrue(entity.getId());
        if (activeMappings > 0) {
            String msg = String.format("Cannot deactivate IntegratedApi id=%d (code=%s): there are %d active mapping(s) linked. Use toggle-status?force=true to force deactivation.",
                    entity.getId(), entity.getCode(), activeMappings);
            log.warn(msg);
            throw new IllegalStateException(msg);
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUser = authentication.getName();

        entity.setIsActive(false);

        entity.setUpdatedBy(currentUser);
        entity.setUpdatedAt(LocalDateTime.now());

        IntegratedApi saved = repository.save(entity);

        log.info("Deactivated integrated API: {} by user: {}", id, currentUser);

        return mapEntityToDto(saved);
    }
    /**
     * Deactivates all active IntegrationMapping records linked to the given integratedApiId.
     * This happens only when force=true is provided.
     *
     * @param integratedApiId the IntegratedApi id
     */
    private void deactivateAssociatedMappings(Long integratedApiId) {
        if (integratedApiId == null) return;

        List<IntegrationMapping> activeMappings = mappingRepository.findByIntegratedApiIdAndIsActiveTrue(integratedApiId);
        if (activeMappings == null || activeMappings.isEmpty()) {
            log.debug("No active mappings found for IntegratedApi {}", integratedApiId);
            return;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUser = authentication.getName();

        log.info("Deactivating {} mapping(s) associated with IntegratedApi {}", activeMappings.size(), integratedApiId);

        for (IntegrationMapping m : activeMappings) {
            m.setIsActive(false);
            m.setUpdatedBy(currentUser);
            m.setUpdatedAt(LocalDateTime.now());
        }

        mappingRepository.saveAll(activeMappings);
        log.info("Deactivated {} mapping(s) for IntegratedApi {} by user: {}",
                activeMappings.size(), integratedApiId, currentUser);
    }
    private void mapRequestToEntity(IntegratedApiRequestDto request, IntegratedApi entity) {
        entity.setCode(request.getCode());
        entity.setName(request.getName());
        entity.setApiUrl(request.getApiUrl());
        entity.setType(IntegratedApi.ApiType.valueOf(request.getType()));
        entity.setIntegratedSystem(request.getIntegratedSystem());
        entity.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);
        entity.setDescription(request.getDescription());
        entity.setBoundApiCode(request.getBoundApiCode());

        entity.setUseOuFromRequest(request.getUseOuFromRequest() != null ? request.getUseOuFromRequest() : false);
        entity.setUsePeFromRequest(request.getUsePeFromRequest() != null ? request.getUsePeFromRequest() : false);
    }
    /**
     * Exports integrated APIs to CSV or Excel bytes according to type.
     */
    public byte[] exportFile(Specification<IntegratedApi> spec, String type, String sortedBy, String sortDirection) throws IOException {
        if ("CSV".equalsIgnoreCase(type)) {
            return convertToCSVStreamed(spec, sortedBy, sortDirection);
        } else if ("Excel".equalsIgnoreCase(type) || "XLSX".equalsIgnoreCase(type)) {
            return convertToExcelStreamed(spec, sortedBy, sortDirection);
        } else {
            throw new IllegalArgumentException("Unsupported export type: " + type);
        }
    }
    private byte[] convertToCSVStreamed(Specification<IntegratedApi> spec, String sortedBy, String sortDirection) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(bos, StandardCharsets.UTF_8))) {
            writer.println("ID,Code,Name,API URL,Type,Integrated System,Active,Description,Created At,Updated At");
            writer.flush();

            int pageSize = 1000;
            int pageNumber = 0;
            boolean hasMore = true;

            while (hasMore) {
                Sort sort = sortDirection.equalsIgnoreCase("asc")
                        ? Sort.by(sortedBy).ascending()
                        : Sort.by(sortedBy).descending();
                Pageable pageable = PageRequest.of(pageNumber, pageSize, sort);
                Page<IntegratedApi> page = repository.findAll(spec, pageable);

                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

                for (IntegratedApi record : page.getContent()) {
                    writer.append(String.valueOf(record.getId())).append(",");
                    writer.append(escapeCsv(record.getCode())).append(",");
                    writer.append(escapeCsv(record.getName())).append(",");
                    writer.append(escapeCsv(record.getApiUrl())).append(",");
                    writer.append(record.getType() != null ? record.getType().toString() : "").append(",");
                    writer.append(escapeCsv(record.getIntegratedSystem())).append(",");
                    writer.append(record.getIsActive() ? "ACTIVE" : "INACTIVE").append(",");
                    writer.append(escapeCsv(record.getDescription())).append(",");
                    writer.append(record.getCreatedAt() != null ? record.getCreatedAt().format(formatter) : "").append(",");
                    writer.append(record.getUpdatedAt() != null ? record.getUpdatedAt().format(formatter) : "").append("\n");
                }
                writer.flush();

                hasMore = page.hasNext();
                pageNumber++;
            }
            writer.flush();
        }

        return bos.toByteArray();
    }
    private byte[] convertToExcelStreamed(Specification<IntegratedApi> spec, String sortedBy, String sortDirection) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        SXSSFWorkbook workbook = new SXSSFWorkbook(100);
        Sheet sheet = workbook.createSheet("Integrated APIs");

        try {
            Row header = sheet.createRow(0);
            String[] columns = {
                    "ID", "Code", "Name", "API URL", "Type",
                    "Integrated System", "Active", "Description",
                    "Created At", "Updated At"
            };
            for (int i = 0; i < columns.length; i++) {
                header.createCell(i).setCellValue(columns[i]);
            }

            int[] widths = {6, 15, 25, 35, 15, 20, 10, 40, 20, 20};
            for (int i = 0; i < widths.length; i++) {
                sheet.setColumnWidth(i, widths[i] * 256);
            }

            int rowIdx = 1;
            int pageSize = 1000;
            int pageNumber = 0;
            boolean hasMore = true;
            final int MAX_CELL_LENGTH = 20000;

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

            while (hasMore) {
                Sort sort = sortDirection.equalsIgnoreCase("asc")
                        ? Sort.by(sortedBy).ascending()
                        : Sort.by(sortedBy).descending();
                Pageable pageable = PageRequest.of(pageNumber, pageSize, sort);
                Page<IntegratedApi> page = repository.findAll(spec, pageable);

                for (IntegratedApi record : page.getContent()) {
                    Row row = sheet.createRow(rowIdx++);
                    row.createCell(0).setCellValue(record.getId() != null ? record.getId() : 0);
                    row.createCell(1).setCellValue(safeString(record.getCode()));
                    row.createCell(2).setCellValue(safeString(record.getName()));
                    row.createCell(3).setCellValue(safeString(record.getApiUrl()));
                    row.createCell(4).setCellValue(record.getType() != null ? record.getType().toString() : "");
                    row.createCell(5).setCellValue(safeString(record.getIntegratedSystem()));
                    row.createCell(6).setCellValue(record.getIsActive() ? "ACTIVE" : "INACTIVE");
                    row.createCell(7).setCellValue(truncate(record.getDescription(), MAX_CELL_LENGTH));
                    row.createCell(8).setCellValue(record.getCreatedAt() != null ? record.getCreatedAt().format(formatter) : "");
                    row.createCell(9).setCellValue(record.getUpdatedAt() != null ? record.getUpdatedAt().format(formatter) : "");
                }

                hasMore = page.hasNext();
                pageNumber++;
            }

            workbook.write(bos);
            return bos.toByteArray();

        } finally {
            workbook.dispose();
        }
    }


    private String safeString(String value) {
        return value != null ? value : "";
    }

    private String truncate(String value, int maxLength) {
        if (value == null) return "";
        if (value.length() <= maxLength) return value;
        return value.substring(0, maxLength - 3) + "...";
    }


    private String escapeCsv(String value) {
        if (value == null) return "";
        String escaped = value.replace("\"", "\"\"");
        if (escaped.contains(",") || escaped.contains("\"") || escaped.contains("\n")) {
            return "\"" + escaped + "\"";
        }
        return escaped;
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
                .boundApiCode(entity.getBoundApiCode())
                .useOuFromRequest(entity.getUseOuFromRequest())
                .usePeFromRequest(entity.getUsePeFromRequest())
                 .createdBy(entity.getCreatedBy())
                .updatedBy(entity.getUpdatedBy())
                .build();
    }
}