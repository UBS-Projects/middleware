package com.middleware.backend.logging.controller;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.middleware.backend.logging.dto.MiddlewareApiCallLogDto;
import com.middleware.backend.logging.model.MiddlewareApiCallLog;
import com.middleware.backend.logging.mapper.MiddlewareApiCallLogMapper;
import com.middleware.backend.logging.repository.MiddlewareApiCallLogRepository;
import com.middleware.backend.spec.MiddlewareApiCallLogSpecification;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.constraints.NotBlank;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import lombok.RequiredArgsConstructor;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/api/logs")
@RequiredArgsConstructor
public class MiddlewareApiCallLogController {

    private final MiddlewareApiCallLogRepository repository;
    private final MiddlewareApiCallLogMapper mapper;
    private final RestTemplate restTemplate = new RestTemplate();

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
            Pageable pageable = PageRequest.of(0, 100_000);
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

    @GetMapping("/attempts/{sourceUUID}")
    @PreAuthorize("permitAll()")
    @Operation(
            summary = "Get all attempts for a source UUID",
            description = "Retrieves all attempts for a given source transaction UUID. Requires 'middlewareLogs:view' authority."
    )
    public ResponseEntity<?> getAllAttempts(@PathVariable("sourceUUID") @NotBlank String sourceUUID) {
        try {
            List<MiddlewareApiCallLog> attempts = repository.findBySourceTransactionUUIDOrderByAttemptNoAsc(sourceUUID.toLowerCase());

            if (attempts.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("status", "ERROR", "message", "No attempts found for given UUID."));
            }

            List<MiddlewareApiCallLogDto> attemptDtos = attempts.stream()
                    .map(mapper::toDto)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(Map.of(
                    "status", "SUCCESS",
                    "sourceTransactionUUID", sourceUUID,
                    "totalAttempts", attempts.size(),
                    "attempts", attemptDtos
            ));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body(Map.of("status", "ERROR", "message", "Failed to retrieve attempts: " + e.getMessage()));
        }
    }
    @PostMapping("/{uuid}/retry")
    @PreAuthorize("permitAll()")
    @Operation(
            summary = "Retry a failed API call",
            description = "Retries a failed API call by sending the same request. Only allowed for completed logs with API errors."
    )
    public ResponseEntity<?> retry(@PathVariable("uuid") @NotBlank String uuid) {
        MiddlewareApiCallLog last = repository.findTopBySourceTransactionUUIDOrderByAttemptNoDesc(uuid.toLowerCase());
        if (last == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("status","ERROR","message","No call found for given UUID."));
        }

        if (!"COMPLETED".equalsIgnoreCase(last.getStatus())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("status","CONFLICT","message","Retry only allowed for completed logs. Current status: " + last.getStatus()));
        }

        boolean isSpecialApi = isSpecialIntegrateApi(last);
        boolean isDryRunFalse = isDryRunFalse(last);

        if (isSpecialApi && isDryRunFalse) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("status","CONFLICT","message","Retry not allowed for integrate API with dry-run=false."));
        }

        boolean hasApiError = (last.getResponseCode() != null && last.getResponseCode() >= 400) ||
                (last.getErrorMessage() != null && !last.getErrorMessage().trim().isEmpty());

        if (!hasApiError) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("status","CONFLICT","message","Retry only allowed for failed API calls. Last response code: " + last.getResponseCode()));
        }

        List<MiddlewareApiCallLog> attempts = repository.findBySourceTransactionUUIDOrderByAttemptNoAsc(uuid.toLowerCase());
        MiddlewareApiCallLog snapshot = attempts.get(0);

        String url = buildUrl(snapshot);
        if (url == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("status","ERROR","message","Cannot reconstruct original URL for retry."));
        }

        HttpMethod method = parseMethod(snapshot.getRequestMethod());
        if (method == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("status","ERROR","message","Unsupported HTTP method for retry."));
        }

        HttpHeaders headers = parseHeaders(snapshot.getRequestHeaders());
        stripHopByHop(headers);
        headers.set("transactionUUID", snapshot.getSourceTransactionUUID());
        headers.set("X-Transaction-UUID", snapshot.getSourceTransactionUUID());
        headers.set("X-Retry-Attempt", "true");

        HttpEntity<String> entity = new HttpEntity<>(snapshot.getRequestBody(), headers);

        ResponseEntity<String> resp;
        try {
            resp = restTemplate.exchange(URI.create(url), method, entity, String.class);
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.ACCEPTED)
                    .body(Map.of("status","QUEUED","message","Retry attempted; check attempts list for new record."));
        }

        return ResponseEntity.ok(Map.of(
                "status", "OK",
                "message", "Retry sent with same request.",
                "httpStatus", resp.getStatusCode().value()
        ));
    }


    private boolean isSpecialIntegrateApi(MiddlewareApiCallLog log) {
        String url = log.getRequestUrl();
        String path = log.getRequestPath();

        if (url != null && url.contains("/camel/external/integrate")) {
            return true;
        }

        if (path != null && path.contains("/camel/external/integrate")) {
            return true;
        }

        String apiEndpoint = log.getApiEndpoint();
        if (apiEndpoint != null && apiEndpoint.contains("/camel/external/integrate")) {
            return true;
        }

        return false;
    }

    private boolean isDryRunFalse(MiddlewareApiCallLog log) {
        String query = log.getRequestQuery();
        String url = log.getRequestUrl();
        String path = log.getRequestPath();

        if (query != null && query.contains("dry-run=false")) {
            return true;
        }

        if (url != null && url.contains("dry-run=false")) {
            return true;
        }

        if (path != null && path.contains("dry-run=false")) {
            return true;
        }

        return false;
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

            for (int i = 0; i < headers.length; i++) sheet.autoSizeColumn(i);

            try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
                workbook.write(bos);
                return bos.toByteArray();
            }
        }
    }

    private String buildUrl(MiddlewareApiCallLog s) {
        if (s.getRequestUrl() != null && !s.getRequestUrl().isBlank()) return s.getRequestUrl();
        if (s.getRequestPath() != null) {
            String q = (s.getRequestQuery() == null || s.getRequestQuery().isBlank()) ? "" : ("?" + s.getRequestQuery());
            return "http://localhost:8081" + s.getRequestPath() + q;
        }
        return null;
    }

    private HttpMethod parseMethod(String m) {
        try { return HttpMethod.valueOf(Objects.toString(m, "GET").toUpperCase()); }
        catch (Exception e) { return null; }
    }

    private HttpHeaders parseHeaders(String mapToString) {
        HttpHeaders h = new HttpHeaders();
        if (mapToString == null) return h;
        String s = mapToString.trim();
        if (s.startsWith("{") && s.endsWith("}")) s = s.substring(1, s.length()-1);
        if (s.isBlank()) return h;
        for (String pair : s.split(",\\s*")) {
            int idx = pair.indexOf('=');
            if (idx > 0) {
                String key = pair.substring(0, idx).trim();
                String val = pair.substring(idx+1).trim();
                if (!key.isEmpty() && !val.isEmpty() && !"null".equalsIgnoreCase(val)) {
                    h.add(key, val);
                }
            }
        }
        return h;
    }

    private void stripHopByHop(HttpHeaders h) {
        List<String> drop = List.of(
                "Host","Content-Length","Transfer-Encoding","Connection",
                "Keep-Alive","Proxy-Authenticate","Proxy-Authorization","TE","Trailer","Upgrade"
        );
        drop.forEach(h::remove);
    }
}