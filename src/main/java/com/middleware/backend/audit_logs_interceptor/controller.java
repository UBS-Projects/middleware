package com.middleware.backend.audit_logs_interceptor;

import com.middleware.backend.audit_logs_interceptor.model.AuditDTO;
import com.middleware.backend.audit_logs_interceptor.model.AuditResponse;
import com.middleware.backend.model.AuditLog;
import com.middleware.backend.repository.AuditLogRepository;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@RestController
@RequestMapping("/auditlogs")
@AllArgsConstructor
public class controller {

    private final AuditLogRepository repo;

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
                    .where(hasField("userName", user, MatchMode.CONTAINS))
                    .and(hasField("method", method, MatchMode.EXACT))
                    .and(hasField("apiPath", apiPath, MatchMode.CONTAINS))
                    .and(hasField("responseStatus", responseStatus, MatchMode.EXACT))
                    .and(hasDateBetween("startTime", startTime, endTime));
            Pageable pageable = PageRequest.of(page, size,
                    sortDirection.equalsIgnoreCase("asc")
                            ? Sort.by(sortedBy).ascending()
                            : Sort.by(sortedBy).descending());

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
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // Simple reusable filter builder
    private <T> Specification<AuditLog> hasField(String fieldName, T value, MatchMode mode) {
        return (root, query, cb) -> {
            if (value == null) return cb.conjunction();
            switch (mode) {
                case CONTAINS -> {
                    return cb.like(cb.lower(root.get(fieldName)), "%" + value.toString().toLowerCase() + "%");
                }
                case EXACT -> {
                    return cb.equal(root.get(fieldName), value);
                }
                default -> {
                    return cb.conjunction();
                }
            }
        };
    }

    private Specification<AuditLog> hasDateBetween(String fieldName, String start, String end) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return (root, query, cb) -> {
            if ((start == null || start.isEmpty()) && (end == null || end.isEmpty())) {
                return cb.conjunction();
            }

            LocalDateTime startDateTime = null;
            LocalDateTime endDateTime = null;

            try {
                if (start != null && !start.isEmpty()) {
                    startDateTime = LocalDateTime.parse(start, formatter);
                }
                if (end != null && !end.isEmpty()) {
                    endDateTime = LocalDateTime.parse(end, formatter);
                }
            } catch (Exception e) {
                return cb.conjunction(); // skip filter if parsing fails
            }

            if (startDateTime == null) {
                return cb.lessThanOrEqualTo(root.get(fieldName), endDateTime);
            }
            if (endDateTime == null) {
                return cb.greaterThanOrEqualTo(root.get(fieldName), startDateTime);
            }

            return cb.between(root.get(fieldName), startDateTime, endDateTime);
        };
    }



    private enum MatchMode {
        CONTAINS,
        EXACT
    }

    @GetMapping("/{id}")
    public ResponseEntity<AuditDTO> getAll(
            @PathVariable("id") Long id
    ) {
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

}
