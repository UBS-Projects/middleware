package com.middleware.backend.audit_logs_interceptor.service;

import com.middleware.backend.audit_logs_interceptor.model.AuditDTO;
import com.middleware.backend.audit_logs_interceptor.model.AuditResponse;
import com.middleware.backend.audit_logs_interceptor.repository.AuditLogRepository;
import com.middleware.backend.audit_logs_interceptor.model.AuditLog;
import lombok.AllArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
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
    public byte[] exportFile(Specification<AuditLog> spec, Pageable pageable, String type) {
        Page<AuditLog> res = repo.findAll(spec, pageable);
        List<AuditLog> data = res.getContent();

        if ("CSV".equalsIgnoreCase(type)) {
            return convertToCSV(data).getBytes(StandardCharsets.UTF_8);
        } else if ("Excel".equalsIgnoreCase(type) || "XLSX".equalsIgnoreCase(type)) {
            try {
                return convertToExcel(data);
            } catch (IOException e) {
                throw new RuntimeException("Error generating Excel file", e);
            }
        } else {
            throw new IllegalArgumentException("Unsupported export type: " + type);
        }
    }

    /**
     * Converts a list of {@link AuditLog} objects to a CSV formatted string.
     *
     * @param logs The list of audit logs to convert.
     * @return A string in CSV format.
     */
    private String convertToCSV(List<AuditLog> logs) {
        StringBuilder sb = new StringBuilder();

        // CSV headers
        sb.append("ID,User Name,Method,API Path,Query String,Response Status,Start Time,End Time,Duration (ms)\n");

        for (AuditLog log : logs) {
            sb.append(log.getId()).append(",");
            sb.append(escapeCsv(log.getUserName())).append(",");
            sb.append(escapeCsv(log.getMethod())).append(",");
            sb.append(escapeCsv(log.getApiPath())).append(",");
            sb.append(escapeCsv(log.getQueryString())).append(",");
            sb.append(log.getResponseStatus() != null ? log.getResponseStatus() : "").append(",");
            sb.append(log.getStartTime() != null ? log.getStartTime().toString() : "").append(",");
            sb.append(log.getEndTime() != null ? log.getEndTime().toString() : "").append(",");
            sb.append(log.getDurationMs() != null ? log.getDurationMs() : "").append("\n");
        }

        return sb.toString();
    }

    /**
     * Escapes special characters in a string for CSV compatibility.
     *
     * @param value The string to escape.
     * @return The escaped string, safe for inclusion in a CSV file.
     */
    private String escapeCsv(String value) {
        if (value == null) return "";
        String escaped = value.replace("\"", "\"\"");
        if (escaped.contains(",") || escaped.contains("\"") || escaped.contains("\n")) {
            return "\"" + escaped + "\"";
        }
        return escaped;
    }

    /**
     * Converts a list of {@link AuditLog} objects to an Excel file (XLSX format).
     *
     * @param logs The list of audit logs to convert.
     * @return A byte array representing the Excel file.
     * @throws IOException if an I/O error occurs during Excel file creation.
     */
    private byte[] convertToExcel(List<AuditLog> logs) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Audit Logs");

            // Header row
            Row header = sheet.createRow(0);
            String[] columns = {
                    "ID", "User Name", "Method", "API Path", "Query String",
                    "Response Status", "Start Time", "End Time", "Duration (ms)"
            };
            for (int i = 0; i < columns.length; i++) {
                header.createCell(i).setCellValue(columns[i]);
            }

            // Data rows
            int rowIdx = 1;
            for (AuditLog log : logs) {
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
            }

            // Autosize columns
            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
            }

            // Write to byte array
            try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
                workbook.write(bos);
                return bos.toByteArray();
            }
        }
    }

    /**
     * Returns the given string or an empty string if the value is null.
     *
     * @param value The string to check.
     * @return The original string or an empty string.
     */
    private String safeString(String value) {
        return value != null ? value : "";
    }
}
