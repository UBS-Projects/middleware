package com.middleware.backend.scheduledJobs.specification;

import com.middleware.backend.scheduledJobs.model.ExecutionHistory;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Predicate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Specification helpers for filtering {@link ExecutionHistory} audit records.
 */
public class JobLogsSpecification {

    public enum MatchMode {
        EXACT, CONTAINS
    }

    /**
     * Generic field filter supporting exact or contains semantics for string fields.
     */
    public static <T> Specification<ExecutionHistory> hasField(String fieldName, Object value, MatchMode matchMode) {
        return (root, query, cb) -> {
            if (value == null) {
                return cb.conjunction();
            }

            if (value instanceof String && ((String) value).isBlank()) {
                return cb.conjunction();
            }

            return switch (matchMode) {
                case EXACT -> cb.equal(root.get(fieldName), value);
                case CONTAINS -> {
                    if (value instanceof String s) {
                        yield cb.like(cb.lower(root.get(fieldName)), "%" + s.toLowerCase() + "%");
                    }
                    yield cb.conjunction(); // CONTAINS only makes sense for Strings
                }
            };
        };
    }

    /**
     * Filters audit records by createdAt between provided dates (inclusive day bounds).
     */
    public static <T> Specification<ExecutionHistory> createdBetween(LocalDate createdBefore, LocalDate createdAfter) {
        return (root, query, cb) -> {
            Predicate predicate = cb.conjunction();

            if (createdAfter != null) {
                LocalDateTime startOfDay = createdAfter.atStartOfDay();
                predicate = cb.and(predicate,
                        cb.greaterThanOrEqualTo(root.get("createdAt"), startOfDay));
            }

            if (createdBefore != null) {
                LocalDateTime endOfDay = createdBefore.atTime(23, 59, 59);
                predicate = cb.and(predicate,
                        cb.lessThanOrEqualTo(root.get("createdAt"), endOfDay));
            }

            return predicate;
        };
    }
}

