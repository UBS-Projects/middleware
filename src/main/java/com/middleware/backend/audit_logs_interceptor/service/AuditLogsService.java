package com.middleware.backend.audit_logs_interceptor.service;

import com.middleware.backend.audit_logs_interceptor.model.AuditDTO;
import com.middleware.backend.audit_logs_interceptor.model.AuditResponse;
import com.middleware.backend.audit_logs_interceptor.repository.AuditLogRepository;
import com.middleware.backend.audit_logs_interceptor.model.AuditLog;
import lombok.AllArgsConstructor;
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
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

/**
 * Service layer for handling business logic related to audit logs.
 * This class provides methods for retrieving, filtering, and exporting audit log data.
 */
@Service
@AllArgsConstructor
public class AuditLogsService {
    private final AuditLogRepository repo;

    /**
     * Retrieves a paginated and filtered list of audit logs.
     * The result is mapped to {@link AuditResponse} objects to provide a summary view.
     *
     * @param spec     A {@link Specification} for dynamic query filtering.
     * @param pageable A {@link Pageable} object for pagination and sorting.
     * @return A {@link Page} of {@link AuditResponse} objects.
     */
    public Page<AuditResponse> getAll(Specification<AuditLog> spec, Pageable pageable) {
        Page<AuditResponse> result = repo.findAll(spec, pageable).map(audit ->
                AuditResponse.builder()
                        .id(audit.getId())
                        .userName(audit.getUserName())
                        .method(audit.getMethod())
                        .apiPath(audit.getApiPath())
                        .responseStatus(audit.getResponseStatus())
                        .startTime(audit.getStartTime())
                        .endTime(audit.getEndTime())
                        .durationMs(audit.getDurationMs())
                        .build()
        );
        return result;
    }


    /**
     * Finds a single audit log by its ID.
     * If found, it returns the full details of the log as an {@link AuditDTO}.
     *
     * @param id The unique identifier of the audit log.
     * @return A {@link ResponseEntity} containing the {@link AuditDTO} if found, or a 404 Not Found response.
     */
    public ResponseEntity<?> findById(Long id) {
        Optional<AuditLog> audit = repo.findById(id);
        if (audit.isPresent()) {
            AuditDTO dto = AuditDTO.builder()
                    .id(audit.get().getId())
                    .userName(audit.get().getUserName())
                    .method(audit.get().getMethod())
                    .apiPath(audit.get().getApiPath())
                    .queryString(audit.get().getQueryString())
                    .responseStatus(audit.get().getResponseStatus())
                    .startTime(audit.get().getStartTime())
                    .endTime(audit.get().getEndTime())
                    .durationMs(audit.get().getDurationMs())
                    .requestHeaders(audit.get().getRequestHeaders())
                    .requestBody(audit.get().getRequestBody())
                    .responseHeaders(audit.get().getResponseHeaders())
                    .responseBody(audit.get().getResponseBody())
                    .build();
            return ResponseEntity.ok(dto);
        }
            else{
                return ResponseEntity.notFound().build();
            }
    }


    /**
     * Exports filtered audit logs to a file (CSV or Excel).
     *
     * @param spec     A {@link Specification} for filtering the logs to be exported.
     * @param pageable A {@link Pageable} object for sorting the exported data.
     * @param type     The desired file format ("CSV" or "Excel").
     * @return A byte array containing the generated file.
     * @throws IllegalArgumentException if the export type is unsupported.
     * @throws RuntimeException if there is an error generating the Excel file.
     */
    public byte[] exportFile(Specification<AuditLog> spec, String type, String sortedBy, String sortDirection) throws IOException {
        if ("CSV".equalsIgnoreCase(type)) {
            return convertToCSVStreamed(spec, sortedBy,sortDirection);
        } else if ("Excel".equalsIgnoreCase(type) || "XLSX".equalsIgnoreCase(type)) {
            return convertToExcelStreamed(spec, sortedBy,sortDirection);
        } else {
            throw new IllegalArgumentException("Unsupported export type: " + type);
        }
    }

    // Streaming CSV - processes in chunks, includes all fields
    private byte[] convertToCSVStreamed(Specification<AuditLog> spec, String sortedBy, String sortDirection) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(bos, StandardCharsets.UTF_8))) {
            // Write headers - all fields
            writer.println("ID,User Name,Method,API Path,Query String,Response Status,Start Time,End Time,Duration (ms)," +
                    "Request Headers,Request Body,Response Headers,Response Body");
            writer.flush();

            int pageSize = 1000;
            int pageNumber = 0;
            boolean hasMore = true;

            while (hasMore) {
                Sort sort = sortDirection.equalsIgnoreCase("asc")
                        ? Sort.by(sortedBy).ascending()
                        : Sort.by(sortedBy).descending();
                Pageable pageable = PageRequest.of(pageNumber, pageSize, sort);
                Page<AuditLog> page = repo.findAll(spec, pageable);

                for (AuditLog log : page.getContent()) {
                    writer.append(String.valueOf(log.getId())).append(",");
                    writer.append(escapeCsv(log.getUserName())).append(",");
                    writer.append(escapeCsv(log.getMethod())).append(",");
                    writer.append(escapeCsv(log.getApiPath())).append(",");
                    writer.append(escapeCsv(log.getQueryString())).append(",");
                    writer.append(log.getResponseStatus() != null ? log.getResponseStatus().toString() : "").append(",");
                    writer.append(log.getStartTime() != null ? log.getStartTime().toString() : "").append(",");
                    writer.append(log.getEndTime() != null ? log.getEndTime().toString() : "").append(",");
                    writer.append(log.getDurationMs() != null ? log.getDurationMs().toString() : "").append(",");
                    writer.append(escapeCsv(log.getRequestHeaders())).append(",");
                    writer.append(escapeCsv(log.getRequestBody())).append(",");
                    writer.append(escapeCsv(log.getResponseHeaders())).append(",");
                    writer.append(escapeCsv(log.getResponseBody())).append("\n");
                }
                writer.flush();

                hasMore = page.hasNext();
                pageNumber++;
            }
            writer.flush();
        }

        return bos.toByteArray();
    }

    // Streaming Excel - includes all fields but truncates LOB content to safe limits
    private byte[] convertToExcelStreamed(Specification<AuditLog> spec, String sortedBy, String sortDirection) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        SXSSFWorkbook workbook = new SXSSFWorkbook(100);
        Sheet sheet = workbook.createSheet("Audit Logs");

        try {
            // Create header row
            Row header = sheet.createRow(0);
            String[] columns = {
                    "ID", "User Name", "Method", "API Path", "Query String",
                    "Response Status", "Start Time", "End Time", "Duration (ms)",
                    "Request Headers", "Request Body", "Response Headers", "Response Body"
            };
            for (int i = 0; i < columns.length; i++) {
                header.createCell(i).setCellValue(columns[i]);
            }

            // Set column widths
            int[] widths = {6, 20, 10, 30, 25, 15, 20, 20, 15, 30, 30, 30, 30};
            for (int i = 0; i < widths.length; i++) {
                sheet.setColumnWidth(i, widths[i] * 256);
            }

            int rowIdx = 1;
            int pageSize = 1000;
            int pageNumber = 0;
            boolean hasMore = true;
            final int MAX_CELL_LENGTH = 20000; // Excel limit is 32767, but keep safe margin

            while (hasMore) {
                Sort sort = sortDirection.equalsIgnoreCase("asc")
                        ? Sort.by(sortedBy).ascending()
                        : Sort.by(sortedBy).descending();
                Pageable pageable = PageRequest.of(pageNumber, pageSize, sort);
                Page<AuditLog> page = repo.findAll(spec, pageable);

                for (AuditLog log : page.getContent()) {
                    Row row = sheet.createRow(rowIdx++);
                    row.createCell(0).setCellValue(log.getId());
                    row.createCell(1).setCellValue(safeString(log.getUserName()));
                    row.createCell(2).setCellValue(safeString(log.getMethod()));
                    row.createCell(3).setCellValue(safeString(log.getApiPath()));
                    row.createCell(4).setCellValue(safeString(log.getQueryString()));
                    row.createCell(5).setCellValue(log.getResponseStatus() != null ? log.getResponseStatus() : 0);
                    row.createCell(6).setCellValue(log.getStartTime() != null ? log.getStartTime().toString() : "");
                    row.createCell(7).setCellValue(log.getEndTime() != null ? log.getEndTime().toString() : "");
                    row.createCell(8).setCellValue(log.getDurationMs() != null ? log.getDurationMs() : 0);

                    // Truncate LOB fields to prevent Excel cell limit (32767 chars)
                    row.createCell(9).setCellValue(truncate(log.getRequestHeaders(), MAX_CELL_LENGTH));
                    row.createCell(10).setCellValue(truncate(log.getRequestBody(), MAX_CELL_LENGTH));
                    row.createCell(11).setCellValue(truncate(log.getResponseHeaders(), MAX_CELL_LENGTH));
                    row.createCell(12).setCellValue(truncate(log.getResponseBody(), MAX_CELL_LENGTH));
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

    /**
     * Truncates a string to a maximum length, appending "..." if truncated.
     * Used to prevent Excel cell content exceeding 32767 character limit.
     *
     * @param value The string to truncate
     * @param maxLength The maximum allowed length
     * @return The truncated string, or empty string if input is null
     */
    private String truncate(String value, int maxLength) {
        if (value == null) return "";
        if (value.length() <= maxLength) return value;
        return value.substring(0, maxLength - 3) + "...";
    }

}
