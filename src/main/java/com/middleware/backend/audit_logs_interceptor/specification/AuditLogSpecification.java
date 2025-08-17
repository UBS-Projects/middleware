package com.middleware.backend.audit_logs_interceptor.specification;

import com.middleware.backend.audit_logs_interceptor.model.AuditLog;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class AuditLogSpecification {
    public static <T> Specification<AuditLog> hasField(String fieldName, T value, MatchMode mode) {
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

    public static Specification<AuditLog> hasDateBetween(String fieldName, String start, String end) {
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



    public enum MatchMode {
        CONTAINS,
        EXACT
    }
}
