package com.middleware.backend.logging.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import com.middleware.backend.logging.model.MiddlewareApiCallLog;
import com.middleware.backend.logging.repository.MiddlewareApiCallLogRepository;
import com.middleware.backend.spec.MiddlewareApiCallLogSpecification;
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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
/**
 * Service to export middleware API call logs to CSV or Excel formats using filters.
 */
public class MiddlewareLogExportService {

    private final MiddlewareApiCallLogRepository repository;

    /**
     * Exports logs filtered by the provided map into CSV or Excel.
     * @param filters key/value filters interpreted by the JPA specification
     * @param type export type: CSV or Excel/XLSX
     * @return HTTP response containing the bytes and appropriate content headers
     */
    public ResponseEntity<byte[]> exportLogs(Map<String, String> filters, String type) {
        try {
            Specification<MiddlewareApiCallLog> spec = MiddlewareApiCallLogSpecification.fromFilters(filters);
            byte[] fileBytes;
            String fileName;
            String contentType;
            String sortValue = filters.get("sort"); // "receivedAt,desc"

            String sortBy = null;
            String sortDirection = null;

            if (sortValue != null && !sortValue.isEmpty()) {
                String[] parts = sortValue.split(",");
                sortBy = parts[0]; // "receivedAt"
                if (parts.length > 1) {
                    sortDirection = parts[1]; // "desc"
                } else {
                    sortDirection = "asc"; // default direction
                }
            }
            if ("CSV".equalsIgnoreCase(type)) {
                fileBytes = convertToCSVStreamed(spec,sortBy,sortDirection);
                fileName = "middleware_logs.csv";
                contentType = "text/csv";
            } else if ("Excel".equalsIgnoreCase(type) || "XLSX".equalsIgnoreCase(type)) {
                fileBytes = convertToExcelStreamed(spec,sortBy,sortDirection);
                fileName = "middleware_logs.xlsx";
                contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            } else {
                throw new IllegalArgumentException("Unsupported export type: " + type);
            }

            if (fileBytes == null || fileBytes.length == 0) {
                return ResponseEntity.noContent().build();
            }

            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=\"" + fileName + "\"")
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(fileBytes);

        } catch (Exception e) {
            log.error("Error exporting middleware logs", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Streams middleware logs to CSV format in chunks to minimize memory usage
     */
    private byte[] convertToCSVStreamed(Specification<MiddlewareApiCallLog> spec, String sortedBy, String sortDirection) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(bos, StandardCharsets.UTF_8))) {
            // Write headers
            writer.println("ID,TransactionID,RouteID,ApiEndpoint,RequestMethod,Status,ResponseCode," +
                    "ReceivedAt,CompletedAt,DurationMs,ClientIP,ApiKeyID,UserEmail,ErrorMessage,RetryCount,SourceTransactionUUID");
            writer.flush();

            int pageSize = 1000;
            int pageNumber = 0;
            boolean hasMore = true;

            while (hasMore) {
                Sort sort = sortDirection.equalsIgnoreCase("asc")
                        ? Sort.by(sortedBy).ascending()
                        : Sort.by(sortedBy).descending();
                Pageable pageable = PageRequest.of(pageNumber, pageSize, sort);
                Page<MiddlewareApiCallLog> page = repository.findAll(spec, pageable);

                for (MiddlewareApiCallLog log : page.getContent()) {
                    writer.append(String.valueOf(log.getId())).append(",");
                    writer.append(escapeCsv(log.getTransactionId())).append(",");
                    writer.append(escapeCsv(log.getRouteId())).append(",");
                    writer.append(escapeCsv(log.getApiEndpoint())).append(",");
                    writer.append(escapeCsv(log.getRequestMethod())).append(",");
                    writer.append(escapeCsv(log.getStatus())).append(",");
                    writer.append(log.getResponseCode() != null ? log.getResponseCode().toString() : "").append(",");
                    writer.append(log.getReceivedAt() != null ? log.getReceivedAt().toString() : "").append(",");
                    writer.append(log.getCompletedAt() != null ? log.getCompletedAt().toString() : "").append(",");
                    writer.append(log.getDurationMs() != null ? log.getDurationMs().toString() : "").append(",");
                    writer.append(escapeCsv(log.getClientIp())).append(",");
                    writer.append(log.getApiKeyId() != null ? log.getApiKeyId().toString() : "").append(",");
                    writer.append(escapeCsv(log.getUserId())).append(",");
                    writer.append(escapeCsv(log.getErrorMessage())).append(",");
                    writer.append(log.getRetryCount() != null ? log.getRetryCount().toString() : "").append(",");
                    writer.append(escapeCsv(log.getSourceTransactionUUID())).append("\n");
                }
                writer.flush();

                hasMore = page.hasNext();
                pageNumber++;
            }
            writer.flush();
        }

        return bos.toByteArray();
    }

    /**
     * Streams middleware logs to Excel format using SXSSFWorkbook to minimize memory usage
     */
    private byte[] convertToExcelStreamed(Specification<MiddlewareApiCallLog> spec, String sortedBy, String sortDirection) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        SXSSFWorkbook workbook = new SXSSFWorkbook(100);
        Sheet sheet = workbook.createSheet("Middleware API Logs");

        try {
            // Create header row
            Row header = sheet.createRow(0);
            String[] headers = {
                    "ID", "TransactionID", "RouteID", "ApiEndpoint", "RequestMethod", "Status", "ResponseCode",
                    "ReceivedAt", "CompletedAt", "DurationMs", "ClientIP", "ApiKeyID", "UserEmail",
                    "ErrorMessage", "RetryCount", "SourceTransactionUUID"
            };
            for (int i = 0; i < headers.length; i++) {
                header.createCell(i).setCellValue(headers[i]);
            }

            // Set column widths
            int[] widths = {8, 20, 15, 30, 15, 12, 12, 20, 20, 12, 15, 12, 20, 25, 12, 30};
            for (int i = 0; i < widths.length; i++) {
                sheet.setColumnWidth(i, widths[i] * 256);
            }

            int rowIdx = 1;
            int pageSize = 1000;
            int pageNumber = 0;
            boolean hasMore = true;
            final int MAX_CELL_LENGTH = 20000;

            while (hasMore) {
                Sort sort = sortDirection.equalsIgnoreCase("asc")
                        ? Sort.by(sortedBy).ascending()
                        : Sort.by(sortedBy).descending();
                Pageable pageable = PageRequest.of(pageNumber, pageSize, sort);
                Page<MiddlewareApiCallLog> page = repository.findAll(spec, pageable);

                for (MiddlewareApiCallLog log : page.getContent()) {
                    Row row = sheet.createRow(rowIdx++);
                    row.createCell(0).setCellValue(log.getId());
                    row.createCell(1).setCellValue(truncate(log.getTransactionId(), MAX_CELL_LENGTH));
                    row.createCell(2).setCellValue(truncate(log.getRouteId(), MAX_CELL_LENGTH));
                    row.createCell(3).setCellValue(truncate(log.getApiEndpoint(), MAX_CELL_LENGTH));
                    row.createCell(4).setCellValue(truncate(log.getRequestMethod(), MAX_CELL_LENGTH));
                    row.createCell(5).setCellValue(truncate(log.getStatus(), MAX_CELL_LENGTH));
                    row.createCell(6).setCellValue(log.getResponseCode() != null ? log.getResponseCode() : 0);
                    row.createCell(7).setCellValue(log.getReceivedAt() != null ? log.getReceivedAt().toString() : "");
                    row.createCell(8).setCellValue(log.getCompletedAt() != null ? log.getCompletedAt().toString() : "");
                    row.createCell(9).setCellValue(log.getDurationMs() != null ? log.getDurationMs() : 0);
                    row.createCell(10).setCellValue(truncate(log.getClientIp(), MAX_CELL_LENGTH));
                    row.createCell(11).setCellValue(log.getApiKeyId() != null ? log.getApiKeyId() : 0);
                    row.createCell(12).setCellValue(truncate(log.getUserId(), MAX_CELL_LENGTH));
                    row.createCell(13).setCellValue(truncate(log.getErrorMessage(), MAX_CELL_LENGTH));
                    row.createCell(14).setCellValue(log.getRetryCount() != null ? log.getRetryCount() : 0);
                    row.createCell(15).setCellValue(truncate(log.getSourceTransactionUUID(), MAX_CELL_LENGTH));
                }

                hasMore = page.hasNext();
                pageNumber++;
            }

            workbook.write(bos);
            return bos.toByteArray();

        } catch (IOException e) {
            throw new RuntimeException("Error generating Excel file", e);
        } finally {
            workbook.dispose();
        }
    }

    /**
     * Escapes special characters in CSV strings for proper formatting
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
     * Truncates a string to prevent Excel cell limit (32767 characters)
     */
    private String truncate(String value, int maxLength) {
        if (value == null) return "";
        if (value.length() <= maxLength) return value;
        return value.substring(0, maxLength - 3) + "...";
    }
}