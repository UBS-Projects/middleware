package com.middleware.backend.integrated_systems.spec;

import com.middleware.backend.integrated_systems.model.IntegratedSystem;
import jakarta.persistence.criteria.Path;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;

/**
 * Specifications for filtering {@link IntegratedSystem} entities using JPA Criteria API.
 * <p>
 * Supports field matching (exact or contains) and date range filtering.
 */
public class IntegratedSystemSpecification {

    /**
     * Builds a specification to filter by a given field.
     *
     * @param fieldName the entity field to filter on
     * @param value     the value to match
     * @param matchMode the match mode (EXACT or CONTAINS)
     * @return a JPA Specification for the field, or null if value is empty
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static Specification<IntegratedSystem> hasField(String fieldName, String value, MatchMode matchMode) {
        return (root, query, criteriaBuilder) -> {
            if (!StringUtils.hasText(value)) return null;

            Path<?> field = root.get(fieldName);
            Class<?> javaType = field.getJavaType();

            switch (matchMode) {
                case EXACT:
                    Object compareValue = value;
                    if (javaType.isEnum()) {
                        try {
                            compareValue = Enum.valueOf((Class<Enum>) javaType, value.trim().toUpperCase());
                        } catch (IllegalArgumentException ex) {
                            return criteriaBuilder.disjunction();
                        }
                    }
                    return criteriaBuilder.equal(field, compareValue);
                case CONTAINS:
                    return criteriaBuilder.like(criteriaBuilder.lower(field.as(String.class)), "%" + value.toLowerCase() + "%");
                default:
                    return null;
            }
        };
    }

    /**
     * Builds a specification to filter entities with a date field greater than or equal to the given date.
     *
     * @param fieldName the entity date field to filter on
     * @param date      the minimum date (inclusive)
     * @return a JPA Specification for the field, or null if date is null
     */
    public static Specification<IntegratedSystem> dateAfter(String fieldName, LocalDate date) {
        return (root, query, criteriaBuilder) -> {
            if (date == null) return null;
            LocalDateTime startOfDay = date.atStartOfDay();
            return criteriaBuilder.greaterThanOrEqualTo(root.get(fieldName), Timestamp.valueOf(startOfDay));
        };
    }

    /**
     * Builds a specification to filter entities with a date field less than or equal to the given date.
     *
     * @param fieldName the entity date field to filter on
     * @param date      the maximum date (inclusive)
     * @return a JPA Specification for the field, or null if date is null
     */
    public static Specification<IntegratedSystem> dateBefore(String fieldName, LocalDate date) {
        return (root, query, criteriaBuilder) -> {
            if (date == null) return null;
            LocalDateTime endOfDay = date.atTime(23, 59, 59);
            return criteriaBuilder.lessThanOrEqualTo(root.get(fieldName), Timestamp.valueOf(endOfDay));
        };
    }

    /**
     * Enum representing how to match string fields in a specification.
     */
    public enum MatchMode {
        /** Exact match */
        EXACT,
        /** Case-insensitive partial match */
        CONTAINS
    }
}
