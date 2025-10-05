//package com.middleware.backend.kaotocamel.controller;
//
//import com.middleware.backend.kaotocamel.dto.MiddlewareResponseDto;
//import com.middleware.backend.kaotocamel.service.MiddlewareProcessorService;
//import io.swagger.v3.oas.annotations.Operation;
//import io.swagger.v3.oas.annotations.Parameter;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.security.access.prepost.PreAuthorize;
//import org.springframework.web.bind.annotation.*;
//
//import jakarta.servlet.http.HttpServletRequest;
//import java.util.HashMap;
//import java.util.Map;
//
///**
// * Controller for handling dynamic middleware API endpoints
// */
//@RestController
//@RequestMapping("/api/middleware")
//@RequiredArgsConstructor
//@Slf4j
//public class MiddlewareController {
//
//    private final MiddlewareProcessorService processorService;
//
//    /**
//     * Dynamic endpoint handler
//     */
//    @GetMapping("/{apiName}")
//    @PreAuthorize("permitAll()")
//    @Operation(summary = "Process Middleware API Request")
//    public ResponseEntity<?> processMiddlewareRequest(
//            @PathVariable String apiName,
//            @Parameter(description = "Period parameter (required). Examples: 202505, THIS_YEAR)")
//            @RequestParam(required = false) String pe,
//            @Parameter(description = "DHIS2 code parameter (required). Example: HMIS_DEV2)")
//            @RequestParam(name = "_dhis2Code", required = false) String dhis2Code,
//            HttpServletRequest request) {
//
//        long startTime = System.currentTimeMillis();
//
//        try {
//            // Validate required parameters
//            if (pe == null || pe.trim().isEmpty()) {
//                return ResponseEntity
//                        .status(HttpStatus.BAD_REQUEST)
//                        .body(createErrorResponse("Parameter 'pe' is required"));
//            }
//            if (dhis2Code == null || dhis2Code.trim().isEmpty()) {
//                return ResponseEntity
//                        .status(HttpStatus.BAD_REQUEST)
//                        .body(createErrorResponse("Parameter '_dhis2Code' is required"));
//            }
//
//            // Construct full API name (with leading slash)
//            String fullApiName = "/" + apiName;
//            log.info("Processing middleware request - API: {}, Period: {}, DHIS2 code: {}",
//                    fullApiName, pe, dhis2Code);
//
//            // Call service with dynamic DHIS2 code
//            MiddlewareResponseDto response = processorService.processMiddlewareRequest(fullApiName, pe, dhis2Code);
//
//            // Log execution time
//            long executionTime = System.currentTimeMillis() - startTime;
//            log.info("Middleware request processed successfully - API: {}, Time: {}ms, Rows: {}",
//                    fullApiName, executionTime, response.getRows().size());
//
//            return ResponseEntity.ok(response);
//
//        } catch (IllegalArgumentException e) {
//            log.error("Validation error for API {}: {}", apiName, e.getMessage());
//            return ResponseEntity
//                    .status(HttpStatus.BAD_REQUEST)
//                    .body(createErrorResponse(e.getMessage()));
//
//        } catch (Exception e) {
//            log.error("Error processing middleware request for API {}: {}", apiName, e.getMessage(), e);
//            return ResponseEntity
//                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
//                    .body(createErrorResponse("Internal server error: " + e.getMessage()));
//        }
//    }
//
//    /**
//     * Create error response
//     */
//    private Map<String, Object> createErrorResponse(String message) {
//        Map<String, Object> error = new HashMap<>();
//        error.put("error", true);
//        error.put("message", message);
//        error.put("timestamp", System.currentTimeMillis());
//        return error;
//    }
//}
