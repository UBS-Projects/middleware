package com.middleware.backend.spec;

import com.middleware.backend.model.ErrorCategory;
import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class ErrorCategorySpecification {

    public enum MatchMode {
        EXACT,
        STARTS_WITH,
        ENDS_WITH,
        CONTAINS
    }

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

    public static Specification<ErrorCategory> hasBooleanField(String fieldName, Boolean value) {
        return (Root<ErrorCategory> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> {
            if (value == null) {
                return null;
            }
            return cb.equal(root.get(fieldName), value);
        };
    }


}
