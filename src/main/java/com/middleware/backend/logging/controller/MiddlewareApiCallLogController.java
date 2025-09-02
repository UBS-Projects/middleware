package com.middleware.backend.logging.controller;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import com.middleware.backend.logging.model.MiddlewareApiCallLog;
import com.middleware.backend.logging.mapper.MiddlewareApiCallLogMapper;
import com.middleware.backend.logging.repository.MiddlewareApiCallLogRepository;
import com.middleware.backend.spec.MiddlewareApiCallLogSpecification;
import io.swagger.v3.oas.annotations.Operation;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/logs")
@RequiredArgsConstructor
public class MiddlewareApiCallLogController {

    private final MiddlewareApiCallLogRepository repository;
    private final MiddlewareApiCallLogMapper mapper;

    @GetMapping
    @PreAuthorize("hasAuthority('middlewareLogs:view')")
    @Operation(
            summary = "List middleware API call logs",
            description = "Retrieves a paginated list of middleware API call logs with optional filters and sorting. Requires 'middlewareLogs:view' authority."
    )
    public ResponseEntity<?> getAll(
            @RequestParam Map<String, String> filters,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(value = "sort", defaultValue = "id,desc") String sortParam) {

        Specification<MiddlewareApiCallLog> spec = MiddlewareApiCallLogSpecification.fromFilters(filters);

        String[] sortFields = sortParam.split(";");
        List<Sort.Order> orders = new java.util.ArrayList<>();
        for (String sortField : sortFields) {
            String[] parts = sortField.split(",");
            if (parts.length == 2) {
                orders.add(new Sort.Order(Sort.Direction.fromString(parts[1]), parts[0]));
            } else {
                orders.add(new Sort.Order(Sort.Direction.ASC, sortField));
            }
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(orders));
        Page<?> result = repository.findAll(spec, pageable).map(mapper::toDto);

        return ResponseEntity.ok(result);
    }

    @GetMapping("/export/{type}")
    @PreAuthorize("hasAuthority('middlewareLogs:export')")
    @Operation(
            summary = "Export middleware API call logs",
            description = "Exports middleware API call logs in CSV or Excel (XLSX) format. Supports filtering. Requires 'middlewareLogs:export' authority."
    )
    public ResponseEntity<byte[]> exportFile(
            @RequestParam Map<String, String> filters,
            @PathVariable("type") String type) {

        try {
            Pageable pageable = PageRequest.of(0, 100_000); // large page for export
            Specification<MiddlewareApiCallLog> spec = MiddlewareApiCallLogSpecification.fromFilters(filters);
            List<MiddlewareApiCallLog> data = repository.findAll(spec, pageable).getContent();

            byte[] fileBytes;
            if ("CSV".equalsIgnoreCase(type)) {
                fileBytes = convertToCSV(data).getBytes(StandardCharsets.UTF_8);
            } else if ("Excel".equalsIgnoreCase(type) || "XLSX".equalsIgnoreCase(type)) {
                fileBytes = convertToExcel(data);
            } else {
                throw new IllegalArgumentException("Unsupported export type: " + type);
            }

            String fileName = "middleware_logs." + (type.equalsIgnoreCase("CSV") ? "csv" : "xlsx");
            String contentType = type.equalsIgnoreCase("CSV")
                    ? "text/csv"
                    : "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=\"" + fileName + "\"")
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(fileBytes);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    private String safeCsv(String value) {
        if (value == null) return "";
        String escaped = value.replace("\"", "\"\"");
        if (escaped.contains(",") || escaped.contains("\"") || escaped.contains("\n")) {
            return "\"" + escaped + "\"";
        }
        return escaped;
    }

    private String convertToCSV(List<MiddlewareApiCallLog> logs) {
        StringBuilder sb = new StringBuilder();
        sb.append("ID,TransactionID,RouteID,ApiEndpoint,RequestMethod,Status,ResponseCode,ReceivedAt,CompletedAt,DurationMs,ClientIP,ApiKeyID,UserEmail,ErrorMessage,RetryCount,SourceTransactionUUID\n");

        for (MiddlewareApiCallLog log : logs) {
            sb.append(log.getId()).append(",");
            sb.append(safeCsv(log.getTransactionId())).append(",");
            sb.append(safeCsv(log.getRouteId())).append(",");
            sb.append(safeCsv(log.getApiEndpoint())).append(",");
            sb.append(safeCsv(log.getRequestMethod())).append(",");
            sb.append(safeCsv(log.getStatus())).append(",");
            sb.append(log.getResponseCode() != null ? log.getResponseCode() : "").append(",");
            sb.append(log.getReceivedAt() != null ? log.getReceivedAt() : "").append(",");
            sb.append(log.getCompletedAt() != null ? log.getCompletedAt() : "").append(",");
            sb.append(log.getDurationMs() != null ? log.getDurationMs() : "").append(",");
            sb.append(safeCsv(log.getClientIp())).append(",");
            sb.append(log.getApiKeyId() != null ? log.getApiKeyId() : "").append(",");
            sb.append(safeCsv(log.getUserId())).append(","); // This now contains user email
            sb.append(safeCsv(log.getErrorMessage())).append(",");
            sb.append(log.getRetryCount() != null ? log.getRetryCount() : "").append(",");
            sb.append(safeCsv(log.getSourceTransactionUUID())).append("\n");
        }

        return sb.toString();
    }

    private byte[] convertToExcel(List<MiddlewareApiCallLog> logs) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Middleware API Logs");

            String[] headers = {
                    "ID","TransactionID","RouteID","ApiEndpoint","RequestMethod","Status","ResponseCode",
                    "ReceivedAt","CompletedAt","DurationMs","ClientIP","ApiKeyID","UserEmail","ErrorMessage","RetryCount","SourceTransactionUUID"
            };
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                headerRow.createCell(i).setCellValue(headers[i]);
            }

            int rowIdx = 1;
            for (MiddlewareApiCallLog log : logs) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(log.getId());
                row.createCell(1).setCellValue(log.getTransactionId() != null ? log.getTransactionId() : "");
                row.createCell(2).setCellValue(log.getRouteId() != null ? log.getRouteId() : "");
                row.createCell(3).setCellValue(log.getApiEndpoint() != null ? log.getApiEndpoint() : "");
                row.createCell(4).setCellValue(log.getRequestMethod() != null ? log.getRequestMethod() : "");
                row.createCell(5).setCellValue(log.getStatus() != null ? log.getStatus() : "");
                row.createCell(6).setCellValue(log.getResponseCode() != null ? log.getResponseCode() : 0);
                row.createCell(7).setCellValue(log.getReceivedAt() != null ? log.getReceivedAt().toString() : "");
                row.createCell(8).setCellValue(log.getCompletedAt() != null ? log.getCompletedAt().toString() : "");
                row.createCell(9).setCellValue(log.getDurationMs() != null ? log.getDurationMs() : 0);
                row.createCell(10).setCellValue(log.getClientIp() != null ? log.getClientIp() : "");
                row.createCell(11).setCellValue(log.getApiKeyId() != null ? log.getApiKeyId() : 0);
                row.createCell(12).setCellValue(log.getUserId() != null ? log.getUserId() : ""); // User email
                row.createCell(13).setCellValue(log.getErrorMessage() != null ? log.getErrorMessage() : "");
                row.createCell(14).setCellValue(log.getRetryCount() != null ? log.getRetryCount() : 0);
                row.createCell(15).setCellValue(log.getSourceTransactionUUID() != null ? log.getSourceTransactionUUID() : "");
            }

            for (int i = 0; i < headers.length; i++) sheet.autoSizeColumn(i);

            try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
                workbook.write(bos);
                return bos.toByteArray();
            }
        }
    }


}