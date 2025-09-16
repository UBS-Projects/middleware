package com.middleware.backend.logging.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import com.middleware.backend.logging.model.MiddlewareApiCallLog;
import com.middleware.backend.logging.repository.MiddlewareApiCallLogRepository;
import com.middleware.backend.spec.MiddlewareApiCallLogSpecification;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class MiddlewareLogExportService {

    private final MiddlewareApiCallLogRepository repository;

    public ResponseEntity<byte[]> exportLogs(Map<String, String> filters, String type) {
        try {
            Pageable pageable = PageRequest.of(0, 100_000);
            Specification<MiddlewareApiCallLog> spec = MiddlewareApiCallLogSpecification.fromFilters(filters);
            List<MiddlewareApiCallLog> data = repository.findAll(spec, pageable).getContent();

            byte[] fileBytes;
            String fileName;
            String contentType;

            if ("CSV".equalsIgnoreCase(type)) {
                fileBytes = convertToCSV(data).getBytes(StandardCharsets.UTF_8);
                fileName = "middleware_logs.csv";
                contentType = "text/csv";
            } else if ("Excel".equalsIgnoreCase(type) || "XLSX".equalsIgnoreCase(type)) {
                fileBytes = convertToExcel(data);
                fileName = "middleware_logs.xlsx";
                contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            } else {
                throw new IllegalArgumentException("Unsupported export type: " + type);
            }

            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=\"" + fileName + "\"")
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(fileBytes);

        } catch (Exception e) {
            log.error("Error exporting logs", e);
            return ResponseEntity.internalServerError().build();
        }
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
            sb.append(safeCsv(log.getUserId())).append(",");
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
                row.createCell(12).setCellValue(log.getUserId() != null ? log.getUserId() : "");
                row.createCell(13).setCellValue(log.getErrorMessage() != null ? log.getErrorMessage() : "");
                row.createCell(14).setCellValue(log.getRetryCount() != null ? log.getRetryCount() : 0);
                row.createCell(15).setCellValue(log.getSourceTransactionUUID() != null ? log.getSourceTransactionUUID() : "");
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
                workbook.write(bos);
                return bos.toByteArray();
            }
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
}