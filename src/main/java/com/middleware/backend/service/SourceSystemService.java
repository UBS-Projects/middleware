package com.middleware.backend.service;

import com.middleware.backend.dto.SourceSystemDto;
import com.middleware.backend.mapper.SourceSystemMapper;
import com.middleware.backend.model.SourceSystem;
import com.middleware.backend.repository.SourceSystemRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityNotFoundException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service layer for managing source systems.
 * <p>
 * Enforces validation rules (trimmed non-empty unique name, prevent deactivation
 * when referenced), handles audit fields from the authenticated user, maps
 * entities to DTOs, and supports CSV/XLSX export.
 */
@Service
@RequiredArgsConstructor
public class SourceSystemService {

    private final SourceSystemRepository sourceSystemRepository;
    private final SourceSystemMapper sourceSystemMapper;

    /**
     * Returns all active source systems as DTOs.
     *
     * @return list of active systems
     */
    public List<SourceSystemDto> getActiveSourceSystems() {
        return sourceSystemRepository.findByActiveTrue().stream()
                .map(sourceSystemMapper::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves a single source system by id.
     *
     * @param id source system id
     * @return the mapped DTO
     * @throws jakarta.persistence.EntityNotFoundException when not found
     */
    public SourceSystemDto getSourceSystemById(Long id) {
        return sourceSystemRepository.findById(id)
                .map(sourceSystemMapper::toDto)
                .orElseThrow(() -> new EntityNotFoundException("Source system not found with id: " + id));
    }

    @Transactional
    /**
     * Creates a new source system after trimming and validating the name and
     * enforcing uniqueness. Sets audit fields using the authenticated user.
     *
     * @param dto input DTO
     * @return created source system as DTO
     * @throws IllegalArgumentException when name is blank or already exists
     */
    public SourceSystemDto createSourceSystem(SourceSystemDto dto) {
        // Trim the name before processing
        String trimmedName = dto.getName() != null ? dto.getName().trim() : null;

        if (trimmedName == null || trimmedName.isEmpty()) {
            throw new IllegalArgumentException("Source system name cannot be null or empty.");
        }

        if (sourceSystemRepository.existsByNameIgnoreCase(trimmedName)) {
            throw new IllegalArgumentException("Source system with name '" + trimmedName + "' already exists.");
        }

        // Set the trimmed name back to dto
        dto.setName(trimmedName);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String emailUser = authentication.getName();
        dto.setCreatedBy(emailUser);
        dto.setUpdatedBy(emailUser);
        dto.setUpdatedAt(LocalDateTime.now());
        SourceSystem sourceSystem = sourceSystemMapper.toEntity(dto);


        SourceSystem savedSourceSystem = sourceSystemRepository.save(sourceSystem);
        return sourceSystemMapper.toDto(savedSourceSystem);
    }

    @Transactional
    /**
     * Updates an existing source system, validating name, uniqueness, and
     * preventing deactivation when mappings exist. Updates audit fields.
     *
     * @param id  source system id
     * @param dto updated values
     * @return updated source system as DTO
     * @throws jakarta.persistence.EntityNotFoundException when the system does not exist
     * @throws IllegalArgumentException when renaming to an existing name or name is blank
     * @throws IllegalStateException when attempting to deactivate a used system
     */
    public SourceSystemDto updateSourceSystem(Long id, SourceSystemDto dto) {
        SourceSystem existingSourceSystem = sourceSystemRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Source system not found with id: " + id));

        // Trim the name before processing
        String trimmedName = dto.getName() != null ? dto.getName().trim() : null;

        if (trimmedName == null || trimmedName.isEmpty()) {
            throw new IllegalArgumentException("Source system name cannot be null or empty.");
        }

        // Check if name is changing and if new name already exists
        if (!existingSourceSystem.getName().equalsIgnoreCase(trimmedName) &&
                sourceSystemRepository.existsByNameIgnoreCase(trimmedName)) {
            throw new IllegalArgumentException("Source system with name '" + trimmedName + "' already exists.");
        }

        // Check if trying to deactivate a source system that is currently active and has error mappings
        if (existingSourceSystem.getActive() && !dto.getActive()) {
            long usageCount = sourceSystemRepository.countErrorMappingsBySourceSystemId(id);
            if (usageCount > 0) {
                throw new IllegalStateException("Cannot deactivate source system with id " + id + " because it is used by " + usageCount + " error mapping(s).");
            }
        }

        existingSourceSystem.setName(trimmedName);
        existingSourceSystem.setDescription(dto.getDescription());
        existingSourceSystem.setActive(dto.getActive());
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String emailUser = authentication.getName();
        existingSourceSystem.setUpdatedBy(emailUser);
        existingSourceSystem.setUpdatedAt(LocalDateTime.now());

        SourceSystem updatedSourceSystem = sourceSystemRepository.save(existingSourceSystem);
        return sourceSystemMapper.toDto(updatedSourceSystem);
    }


    @Transactional
    /**
     * Toggles the active status of a source system, ensuring deactivation is
     * not allowed when the system is referenced by mappings. Updates audit fields.
     *
     * @param id source system id
     * @return updated source system as DTO
     * @throws jakarta.persistence.EntityNotFoundException when the system does not exist
     * @throws IllegalStateException when attempting to deactivate a used system
     */
    public SourceSystemDto toggleSourceSystem(Long id) {
        SourceSystem sourceSystem = sourceSystemRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Source system not found with id: " + id));

        // If trying to deactivate an active source system, check for error mappings
        if (sourceSystem.getActive()) {
            long usageCount = sourceSystemRepository.countErrorMappingsBySourceSystemId(id);
            if (usageCount > 0) {
                throw new IllegalStateException("Cannot deactivate source system with id " + id + " because it is used by " + usageCount + " error mapping(s).");
            }
        }

        sourceSystem.setActive(!sourceSystem.getActive());
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String emailUser = authentication.getName();
        sourceSystem.setUpdatedBy(emailUser);
        sourceSystem.setUpdatedAt(LocalDateTime.now());
        SourceSystem savedSourceSystem = sourceSystemRepository.save(sourceSystem);
        return sourceSystemMapper.toDto(savedSourceSystem);
    }

    /**
     * Retrieves a page of source systems matching the given specification.
     *
     * @param spec     specification with optional filters
     * @param pageable pagination and sorting options
     * @return page of matching systems
     */
    public Page<SourceSystem> getAllSourceSystems(Specification<SourceSystem> spec, Pageable pageable) {
        return sourceSystemRepository.findAll(spec, pageable);
    }

    /**
     * Exports source systems matching the specification into CSV or XLSX format.
     *
     * @param spec     filters to apply
     * @param pageable sort/pagination used to bound and order the export
     * @param type     export type: "CSV", "Excel", or "XLSX"
     * @return file bytes
     * @throws IOException              on I/O errors
     * @throws IllegalArgumentException when an unsupported type is requested
     */
    public byte[] exportFile(Specification<SourceSystem> spec, String type, String sortedBy, String sortDir) throws IOException {
        if ("CSV".equalsIgnoreCase(type)) {
            return convertToCSVStreamed(spec, sortedBy, sortDir);
        } else if ("Excel".equalsIgnoreCase(type) || "XLSX".equalsIgnoreCase(type)) {
            return convertToExcelStreamed(spec, sortedBy, sortDir);
        } else {
            throw new IllegalArgumentException("Unsupported export type: " + type);
        }
    }


    // Convert List<SourceSystem> to CSV string
    /**
     * Converts a list of source systems into a CSV string.
     *
     * @param systems list to serialize
     * @return CSV contents
     */
    private byte[] convertToCSVStreamed(Specification<SourceSystem> spec, String sortedBy, String sortDir) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(bos, StandardCharsets.UTF_8))) {
            // CSV header
            writer.println("ID,Name,Description,Active,Created At,Updated At");
            writer.flush();

            int pageSize = 1000;
            int pageNumber = 0;
            boolean hasMore = true;

            while (hasMore) {
                Sort sort = sortDir.equalsIgnoreCase("asc")
                        ? Sort.by(sortedBy).ascending()
                        : Sort.by(sortedBy).descending();
                Pageable pageable = PageRequest.of(pageNumber, pageSize, sort);
                Page<SourceSystem> page = sourceSystemRepository.findAll(spec, pageable);

                for (SourceSystem sys : page.getContent()) {
                    writer.append(String.valueOf(sys.getId())).append(",");
                    writer.append(escapeCsv(sys.getName())).append(",");
                    writer.append(escapeCsv(sys.getDescription())).append(",");
                    writer.append(sys.getActive() != null ? sys.getActive().toString() : "").append(",");
                    writer.append(sys.getCreatedAt() != null ? sys.getCreatedAt().toString() : "").append(",");
                    writer.append(sys.getUpdatedAt() != null ? sys.getUpdatedAt().toString() : "").append("\n");
                }

                writer.flush();
                hasMore = page.hasNext();
                pageNumber++;
            }

            writer.flush();
        }

        return bos.toByteArray();
    }


    // CSV escaping helper
    /**
     * Escapes a CSV field by doubling quotes and quoting when necessary.
     *
     * @param value raw field value
     * @return escaped CSV field
     */
    private String escapeCsv(String value) {
        if (value == null) return "";
        String escaped = value.replace("\"", "\"\"");
        if (escaped.contains(",") || escaped.contains("\"") || escaped.contains("\n")) {
            return "\"" + escaped + "\"";
        }
        return escaped;
    }

    private String safeString(String value) {
        return value != null ? value : "";
    }

    private String truncate(String value, int maxLength) {
        if (value == null) return "";
        if (value.length() <= maxLength) return value;
        return value.substring(0, maxLength - 3) + "...";
    }


    // Convert List<SourceSystem> to Excel bytes
    /**
     * Converts a list of source systems into an XLSX workbook and returns its bytes.
     *
     * @param systems list to serialize
     * @return xlsx bytes
     * @throws IOException if writing fails
     */
    private byte[] convertToExcelStreamed(Specification<SourceSystem> spec, String sortedBy, String sortDir) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        SXSSFWorkbook workbook = new SXSSFWorkbook(100);
        Sheet sheet = workbook.createSheet("Source Systems");

        try {
            // Header row
            Row header = sheet.createRow(0);
            String[] columns = { "ID", "Name", "Description", "Active", "Created At", "Updated At" };
            for (int i = 0; i < columns.length; i++) {
                header.createCell(i).setCellValue(columns[i]);
            }

            int[] widths = { 10, 25, 40, 10, 25, 25 };
            for (int i = 0; i < widths.length; i++) {
                sheet.setColumnWidth(i, widths[i] * 256);
            }

            int rowIdx = 1;
            int pageSize = 1000;
            int pageNumber = 0;
            boolean hasMore = true;
            final int MAX_CELL_LENGTH = 20000;

            while (hasMore) {
                Sort sort = sortDir.equalsIgnoreCase("asc")
                        ? Sort.by(sortedBy).ascending()
                        : Sort.by(sortedBy).descending();
                Pageable pageable = PageRequest.of(pageNumber, pageSize, sort);
                Page<SourceSystem> page = sourceSystemRepository.findAll(spec, pageable);

                for (SourceSystem sys : page.getContent()) {
                    Row row = sheet.createRow(rowIdx++);
                    row.createCell(0).setCellValue(sys.getId());
                    row.createCell(1).setCellValue(safeString(sys.getName()));
                    row.createCell(2).setCellValue(truncate(sys.getDescription(), MAX_CELL_LENGTH));
                    row.createCell(3).setCellValue(sys.getActive() != null ? sys.getActive() : false);
                    row.createCell(4).setCellValue(sys.getCreatedAt() != null ? sys.getCreatedAt().toString() : "");
                    row.createCell(5).setCellValue(sys.getUpdatedAt() != null ? sys.getUpdatedAt().toString() : "");
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

}