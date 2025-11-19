package com.middleware.backend.kaotocamel.controller;

import com.middleware.backend.kaotocamel.dto.MiddlewareResponseDto;
import com.middleware.backend.kaotocamel.service.MiddlewareProcessorService;
import com.middleware.backend.kaotocamel.service.Dhis2ClientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Controller for handling dynamic middleware API endpoints with auto URL generation
 */
@RestController
@RequestMapping("/api/middleware")
@RequiredArgsConstructor
@Slf4j
public class MiddlewareController {

    private final MiddlewareProcessorService processorService;
    private final Dhis2ClientService dhis2Client;

    /**
     * NEW: Direct Analytics endpoint with URL auto-generation
     */
    @GetMapping("/analytics")
    @PreAuthorize("permitAll()")
    @Operation(summary = "Execute DHIS2 Analytics with auto-generated URL")
    public ResponseEntity<?> executeAnalytics(
            @Parameter(description = "DHIS2 code (required)", example = "HMIS_DEV2")
            @RequestParam(name = "_dhis2Code", required = true) String dhis2Code,

            @Parameter(description = "Data dimensions - semicolon separated (required)",
                    example = "mCryL70xgcR;mCryL70xgcR.koh0tliu6RV;eeGloPJ7lGX")
            @RequestParam(required = true) String dx,

            @Parameter(description = "Organization units - semicolon separated (optional)",
                    example = "CI1vsTW2OMP")
            @RequestParam(required = false) String ou,

            @Parameter(description = "Periods - semicolon separated (optional)",
                    example = "202507")
            @RequestParam(required = false) String pe,

            @Parameter(description = "Output ID scheme (optional)", example = "name")
            @RequestParam(required = false) String outputIdScheme,

            @Parameter(description = "Skip rounding (optional)", example = "false")
            @RequestParam(required = false) String skipRounding,

            @Parameter(description = "Completed only (optional)", example = "true")
            @RequestParam(required = false) String completedOnly,

            @Parameter(description = "Additional attribute dimensions (format: UID=value)")
            @RequestParam(required = false) Map<String, String> attributes) {

        long startTime = System.currentTimeMillis();

        try {
            // Validate mandatory params
            if (dhis2Code == null || dhis2Code.trim().isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("status", "ERROR");
                errorResponse.put("message", "Missing Required Parameters: Parameter '_dhis2Code' is required.");

                return ResponseEntity
                        .status(HttpStatus.BAD_REQUEST)
                        .body(errorResponse);
            }

            if (dx == null || dx.trim().isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("status", "ERROR");
                errorResponse.put("message", "Missing Required Parameters: Parameter 'dx' (Data Dimension) is required.");

                return ResponseEntity
                        .status(HttpStatus.BAD_REQUEST)
                        .body(errorResponse);
            }

            // Build other params map
            Map<String, String> otherParams = new LinkedHashMap<>();
            if (outputIdScheme != null && !outputIdScheme.trim().isEmpty()) {
                otherParams.put("outputIdScheme", outputIdScheme);
            }
            if (skipRounding != null && !skipRounding.trim().isEmpty()) {
                otherParams.put("skipRounding", skipRounding);
            }
            if (completedOnly != null && !completedOnly.trim().isEmpty()) {
                otherParams.put("completedOnly", completedOnly);
            }

            // Extract attribute dimensions
            Map<String, String> attributeDimensions = new LinkedHashMap<>();
            if (attributes != null) {
                Set<String> knownParams = Set.of("_dhis2Code", "dx", "ou", "pe",
                        "outputIdScheme", "skipRounding", "completedOnly");

                attributes.forEach((key, value) -> {
                    if (!knownParams.contains(key) && key.length() == 11) {
                        attributeDimensions.put(key, value);
                    }
                });
            }

            log.info("Executing Analytics - DHIS2: {}, dx: {}, ou: {}, pe: {}, attrs: {}",
                    dhis2Code, dx, ou, pe, attributeDimensions.size());

            Map<String, Object> response = dhis2Client.executeAnalyticsCall(
                    dhis2Code, dx, ou, pe, attributeDimensions, otherParams
            );

            long executionTime = System.currentTimeMillis() - startTime;
            log.info("Analytics executed successfully in {}ms", executionTime);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.error("Validation error: {}", e.getMessage());

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "ERROR");
            errorResponse.put("message", "Failed to execute analytics: " + e.getMessage());

            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(errorResponse);

        } catch (Exception e) {
            log.error("Error executing analytics: {}", e.getMessage(), e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "ERROR");
            errorResponse.put("message", "Failed to execute analytics: " + e.getMessage());

            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorResponse);
        }
    }
    /**
     * Legacy middleware endpoint
     * NOW: pe and ou are OPTIONAL - will be validated in service based on API configuration
     */
    @GetMapping("/{apiName}")
    @PreAuthorize("permitAll()")
    @Operation(summary = "Process Middleware API Request")
    public ResponseEntity<?> processMiddlewareRequest(
            @PathVariable String apiName,
            @Parameter(description = "Period parameter (optional if configured in API)")
            @RequestParam(required = false) String pe,
            @Parameter(description = "Organization unit parameter (optional if configured in API)")
            @RequestParam(required = false) String ou,
            @Parameter(description = "DHIS2 code parameter (required)")
            @RequestParam(name = "_dhis2Code", required = false) String dhis2Code) {

        long startTime = System.currentTimeMillis();

        try {
            // Only dhis2Code is strictly required here
            if (dhis2Code == null || dhis2Code.trim().isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("error", "Missing Parameter");
                errorResponse.put("message", "⚠️ Parameter '_dhis2Code' is required");
                errorResponse.put("timestamp", System.currentTimeMillis());

                return ResponseEntity
                        .status(HttpStatus.BAD_REQUEST)
                        .body(errorResponse);
            }

            String fullApiName = "/" + apiName;
            log.info("Processing middleware request - API: {}, Period: {}, OU: {}, DHIS2: {}",
                    fullApiName, pe, ou, dhis2Code);

             MiddlewareResponseDto response = processorService.processMiddlewareRequest(
                    fullApiName, pe, ou, dhis2Code);

            long executionTime = System.currentTimeMillis() - startTime;
            log.info("Middleware request processed - API: {}, Time: {}ms, Rows: {}",
                    fullApiName, executionTime, response.getRows().size());

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
             log.error("Validation error for API {}: {}", apiName, e.getMessage());

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Validation Error");
            errorResponse.put("message", e.getMessage());
            errorResponse.put("timestamp", System.currentTimeMillis());

            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(errorResponse);

        } catch (Exception e) {
            log.error("Error processing middleware request for API {}: {}", apiName, e.getMessage(), e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Internal Server Error");
            errorResponse.put("message", "Internal server error: " + e.getMessage());
            errorResponse.put("timestamp", System.currentTimeMillis());

            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorResponse);
        }
    }

    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("error", true);
        error.put("message", message);
        error.put("timestamp", System.currentTimeMillis());
        return error;
    }
}