package com.middleware.backend.spec;

import com.middleware.backend.model.ErrorCategory;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;


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
}
