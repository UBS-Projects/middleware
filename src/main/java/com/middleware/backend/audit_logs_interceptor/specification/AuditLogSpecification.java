package com.middleware.backend.audit_logs_interceptor.specification;

import com.middleware.backend.audit_logs_interceptor.model.AuditLog;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Provides {@link Specification}s for querying {@link AuditLog} entities.
 * This class contains static methods for creating dynamic, reusable query conditions
 * based on different fields and criteria, such as text matching and date ranges.
 */
public class AuditLogSpecification {
    /**
     * Creates a {@link Specification} for filtering by a specific field and value.
     * Supports different matching modes, such as exact match or contains (case-insensitive).
     *
     * @param <T>       The type of the value being matched.
     * @param fieldName The name of the entity field to filter on.
     * @param value     The value to match against. If null, the specification will be a no-op.
     * @param mode      The matching mode to use (e.g., {@link MatchMode#EXACT} or {@link MatchMode#CONTAINS}).
     * @return A {@link Specification} that can be used in a JPA query.
     */
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

    /**
     * Creates a {@link Specification} for filtering entities based on a date range.
     * The date strings are parsed using the format "yyyy-MM-dd HH:mm:ss".
     *
     * @param fieldName The name of the date/time field in the entity.
     * @param start     The start of the date range as a string. Can be null.
     * @param end       The end of the date range as a string. Can be null.
     * @return A {@link Specification} for the date range query. If parsing fails, a no-op specification is returned.
     */
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


    /**
     * Defines the modes for string matching in specifications.
     */
    public enum MatchMode {
        /** Case-insensitive containment check. */
        CONTAINS,
        /** Exact match. */
        EXACT
    }
}
