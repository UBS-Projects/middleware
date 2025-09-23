package com.middleware.backend.errormapping.spec;

import java.time.LocalDate;

import org.springframework.data.jpa.domain.Specification;

import com.middleware.backend.errormapping.model.ErrorCategory;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

/**
 * <p>
 * Provides case-insensitive string matching with multiple modes, boolean field
 * filtering, and inclusive createdAt date range filtering.
 */
public class ErrorCategorySpecification {

    /**
     * Modes for matching string fields in criteria.
     */
    public enum MatchMode {
        EXACT, STARTS_WITH, ENDS_WITH, CONTAINS
    }

    /**
     * Builds a specification for matching a string field by the provided mode.
     * Returns null when the value is null/blank so it can be ignored.
     *
     * @param fieldName entity attribute to filter
     * @param value     value to match (case-insensitive)
     * @param matchMode how to match the value (exact/starts/ends/contains)
     * @return specification or null if value is blank
     */
    public static Specification<ErrorCategory> hasField(String fieldName, String value, MatchMode matchMode) {
        return (Root<ErrorCategory> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> {
            if (value == null || value.trim().isEmpty()) {
                return null; // return null so Spring ignores this spec
            }

            String pattern;
            switch (matchMode) {
            case CONTAINS:
                pattern = "%" + value + "%";
                return cb.like(cb.lower(root.get(fieldName)), pattern.toLowerCase());
            case STARTS_WITH:
                pattern = value + "%";
                return cb.like(cb.lower(root.get(fieldName)), pattern.toLowerCase());
            case ENDS_WITH:
                pattern = "%" + value;
                return cb.like(cb.lower(root.get(fieldName)), pattern.toLowerCase());
            case EXACT:
            default:
                return cb.equal(cb.lower(root.get(fieldName)), value.toLowerCase());
            }
        };
    }

    /**
     * Filters by the createdAt timestamp within an inclusive date range.
     * Returns null when both dates are null.
     *
     * @param start inclusive start date (00:00:00)
     * @param end   inclusive end date (23:59:59)
     * @return specification for the date range or null when both are null
     */
    public static Specification<ErrorCategory> createdBetween(LocalDate start, LocalDate end) {
        return (root, query, cb) -> {
            if (start == null && end == null) {
                return null;
            }
            if (start != null && end != null) {
                return cb.between(root.get("createdAt"), start.atStartOfDay(), end.atTime(23, 59, 59));
            }
            if (start != null) {
                return cb.greaterThanOrEqualTo(root.get("createdAt"), start.atStartOfDay());
            }
            return cb.lessThanOrEqualTo(root.get("createdAt"), end.atTime(23, 59, 59));
        };
    }

    /**
     * Filters a boolean field by equality. Returns null when value is null.
     *
     * @param fieldName boolean attribute to filter
     * @param value     desired value; when null, filter is ignored
     * @return specification or null
     */
    public static Specification<ErrorCategory> hasBooleanField(String fieldName, Boolean value) {
        return (Root<ErrorCategory> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> {
            if (value == null) {
                return null;
            }
            return cb.equal(root.get(fieldName), value);
        };
    }

}
