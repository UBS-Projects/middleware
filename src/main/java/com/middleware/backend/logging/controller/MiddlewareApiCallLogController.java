package com.middleware.backend.logging.controller;

import java.util.Map;

import com.middleware.backend.logging.service.MiddlewareApiCallLogService;
import com.middleware.backend.logging.service.MiddlewareLogExportService;
import com.middleware.backend.logging.service.MiddlewareLogRetryService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/logs")
@RequiredArgsConstructor
/**
 * REST controller exposing endpoints to view, export, and retry middleware API call logs.
 * Secured via method-level authorities for viewing, exporting, and retrying.
 */
public class MiddlewareApiCallLogController {

    private final MiddlewareApiCallLogService logService;
    private final MiddlewareLogExportService exportService;
    private final MiddlewareLogRetryService retryService;

    @GetMapping
    /**
     * Retrieves a paginated list of API call logs with optional filters and sorting.
     * @param filters map of filter keys/values consumed by the specification builder
     * @param page page index (0-based)
     * @param size page size
     * @param sortParam sort expression like "id,desc" or "field,asc;field2,desc"
     * @return page of log DTOs
     */
    @PreAuthorize("hasAuthority('middlewareLogs:view')")
    @Operation(
            summary = "List middleware API call logs",
            description = "Retrieves a paginated list of middleware API call logs with optional filters and sorting. Requires 'middlewareLogs:view' authority."
    )
    public ResponseEntity<Page<?>> getAll(
            @RequestParam Map<String, String> filters,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(value = "sort", defaultValue = "id,desc") String sortParam) {

        Page<?> result = logService.getAllLogs(filters, page, size, sortParam);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/export/{type}")
    /**
     * Exports logs to CSV or Excel format based on path variable.
     * @param filters filter map for narrowing results
     * @param type export type: CSV or Excel/XLSX
     * @return file response with appropriate content type
     */
    @PreAuthorize("hasAuthority('middlewareLogs:export')")
    @Operation(
            summary = "Export middleware API call logs",
            description = "Exports middleware API call logs in CSV or Excel (XLSX) format. Supports filtering. Requires 'middlewareLogs:export' authority."
    )
    public ResponseEntity<byte[]> exportFile(
            @RequestParam Map<String, String> filters,
            @PathVariable("type") String type) {

        return exportService.exportLogs(filters, type);
    }

    @GetMapping("/attempts/{sourceUUID}")
    /**
     * Returns all attempts recorded for a given transaction UUID.
     * @param sourceUUID transaction UUID
     * @return attempts array with metadata
     */
    @PreAuthorize("hasAuthority('middlewareLogs:view')")
    @Operation(
            summary = "Get all attempts for a source UUID",
            description = "Retrieves all attempts for a given source transaction UUID. Requires 'middlewareLogs:view' authority."
    )
    public ResponseEntity<?> getAllAttempts(@PathVariable("sourceUUID") @NotBlank String sourceUUID) {
        return logService.getAllAttempts(sourceUUID);
    }

    @PostMapping("/{uuid}/retry")
    /**
     * Retries a failed API call by resending the original request.
     * @param uuid transaction UUID
     * @return retry dispatch result
     */
    @PreAuthorize("hasAuthority('middlewareLogs:retry')")
    @Operation(
            summary = "Retry a failed API call",
            description = "Retries a failed API call by sending the same request. Only allowed for completed logs with API errors."
    )
    public ResponseEntity<?> retry(@PathVariable("uuid") @NotBlank String uuid) {
        return retryService.retryApiCall(uuid);
    }
}