package com.middleware.backend.camel.spec;

import com.middleware.backend.camel.model.DynamicRouteAudit;
import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Helper factory for building JPA Specifications targeting {@link DynamicRouteAudit}.
 */
public class DynamicRouteLogsSpecification {
    /**
     * String-matching mode.
     */
    public enum MatchMode {
        EXACT, CONTAINS
    }

    /**
     * Creates a specification that matches a string field by exact or contains mode.
     * Null/blank values are ignored (returning a no-op conjunction).
     *
     * @param fieldName entity field name to compare
     * @param value string value to match
     * @param matchMode EXACT equals or CONTAINS like '%value%'
     * @return a specification for the given condition
     */
    public static <T> Specification<DynamicRouteAudit> hasField(String fieldName, String value, MatchMode matchMode) {
        return (Root<DynamicRouteAudit> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> {
            if (value == null || value.trim().isEmpty()) {
                return cb.conjunction();
            }
            if (matchMode == MatchMode.EXACT) {
                return cb.equal(root.get(fieldName), value);
            } else { // CONTAINS
                return cb.like(cb.lower(root.get(fieldName)), "%" + value.toLowerCase() + "%");
            }
        };
    }

    /**
     * Creates a date range specification on the audit timestamp field.
     * If both start and end are null, returns a no-op conjunction.
     * End date is treated as inclusive (23:59:59 of that day).
     *
     * @param startDate inclusive start date or null
     * @param endDate inclusive end date or null
     * @return a specification filtering by the computed date-time range
     */
    public static <T> Specification<DynamicRouteAudit> createdBetween(LocalDate startDate, LocalDate endDate) {
        return (Root<DynamicRouteAudit> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> {
            if (startDate == null && endDate == null) {
                return cb.conjunction();
            }

            LocalDateTime startDateTime = null;
            LocalDateTime endDateTime = null;

            if (startDate != null) {
                startDateTime = startDate.atStartOfDay();
            }
            if (endDate != null) {
                // include the whole end day
                endDateTime = endDate.atTime(23, 59, 59);
            }

            if (startDateTime != null && endDateTime != null) {
                return cb.between(root.get("timestamp"), startDateTime, endDateTime);
            } else if (startDateTime != null) {
                return cb.greaterThanOrEqualTo(root.get("timestamp"), startDateTime);
            } else {
                return cb.lessThanOrEqualTo(root.get("timestamp"), endDateTime);
            }
        };
    }
}