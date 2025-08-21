package com.middleware.backend.users.specification;


import com.middleware.backend.model.ErrorCategory;
import com.middleware.backend.model.SourceSystem;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class SourceSystemSpecification {

    public enum MatchMode {
        EQUALS,
        CONTAINS,
        STARTS_WITH,
        ENDS_WITH
    }

    public static Specification<SourceSystem> hasField(String fieldName, String value, MatchMode matchMode) {
        return (root, query, cb) -> {
            if (value == null || value.trim().isEmpty()) {
                return null;
            }
            String pattern = value;
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
                default:
                    return cb.equal(cb.lower(root.get(fieldName)), value.toLowerCase());
            }
        };
    }

    public static Specification<SourceSystem> hasBooleanField(String fieldName, Boolean value) {
        return (root, query, cb) -> {
            if (value == null) {
                return null;
            }
            return cb.equal(root.get(fieldName), value);
        };
    }

    public static Specification<SourceSystem> createdBetween(LocalDate start, LocalDate end) {
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
}

