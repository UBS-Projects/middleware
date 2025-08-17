package com.middleware.backend.audit_logs_interceptor.controller;

import com.middleware.backend.audit_logs_interceptor.model.AuditResponse;
import com.middleware.backend.audit_logs_interceptor.service.AuditLogsService;
import com.middleware.backend.audit_logs_interceptor.specification.AuditLogSpecification;
import com.middleware.backend.audit_logs_interceptor.model.AuditLog;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auditlogs")
@AllArgsConstructor
public class AuditLogsController {

    private final AuditLogsService service;

    @GetMapping("")
    public ResponseEntity<Page<AuditResponse>> getAll(
            @RequestParam(required = false) String user,
            @RequestParam(required = false) String method,
            @RequestParam(required = false) String apiPath,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            @RequestParam(required = false) Integer responseStatus,
            @RequestParam(required = false, defaultValue = "startTime") String sortedBy,
            @RequestParam(defaultValue = "desc") String sortDirection,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        try {
            Specification<AuditLog> spec = Specification
                    .where(AuditLogSpecification.hasField("userName", user, AuditLogSpecification.MatchMode.CONTAINS))
                    .and(AuditLogSpecification.hasField("method", method, AuditLogSpecification.MatchMode.EXACT))
                    .and(AuditLogSpecification.hasField("apiPath", apiPath, AuditLogSpecification.MatchMode.CONTAINS))
                    .and(AuditLogSpecification.hasField("responseStatus", responseStatus, AuditLogSpecification.MatchMode.EXACT))
                    .and(AuditLogSpecification.hasDateBetween("startTime", startTime, endTime));
            Pageable pageable = PageRequest.of(page, size,
                    sortDirection.equalsIgnoreCase("asc")
                            ? Sort.by(sortedBy).ascending()
                            : Sort.by(sortedBy).descending());


            Page<AuditResponse> result = service.getAll(spec,pageable);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // Simple reusable filter builder


    @GetMapping("/{id}")
    public ResponseEntity<?> getAll(
            @PathVariable("id") Long id) {
        return service.findById(id);
    }



//    ********************************************** Export File ***************************************************

    @GetMapping("/export/{type}")
    public ResponseEntity<byte[]> export(
            @RequestParam(required = false) String user,
            @RequestParam(required = false) String method,
            @RequestParam(required = false) String apiPath,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            @RequestParam(required = false) Integer responseStatus,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false, defaultValue = "startTime") String sortedBy,
            @RequestParam(defaultValue = "desc") String sortDirection,
            @PathVariable("type") String type
    ) {
        try {
            Specification<AuditLog> spec = Specification
                    .where(AuditLogSpecification.hasField("userName", user, AuditLogSpecification.MatchMode.CONTAINS))
                    .and(AuditLogSpecification.hasField("method", method, AuditLogSpecification.MatchMode.EXACT))
                    .and(AuditLogSpecification.hasField("apiPath", apiPath, AuditLogSpecification.MatchMode.CONTAINS))
                    .and(AuditLogSpecification.hasField("responseStatus", responseStatus, AuditLogSpecification.MatchMode.EXACT))
                    .and(AuditLogSpecification.hasDateBetween("startTime", startTime, endTime));
            Pageable pageable = PageRequest.of(page, size,
                    sortDirection.equalsIgnoreCase("asc")
                            ? Sort.by(sortedBy).ascending()
                            : Sort.by(sortedBy).descending());




            byte[] fileBytes = service.exportFile(spec, pageable, type);


            String fileName = "audit_logs." + (type.equalsIgnoreCase("CSV") ? "csv" : "xlsx");
            String contentType = type.equalsIgnoreCase("CSV")
                    ? "text/csv"
                    : "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=\"" + fileName + "\"")
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(fileBytes);


        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }


}
