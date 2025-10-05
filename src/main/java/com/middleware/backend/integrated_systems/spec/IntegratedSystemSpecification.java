package com.middleware.backend.integrated_systems.spec;

import com.middleware.backend.integrated_systems.model.IntegratedSystem;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

public class IntegratedSystemSpecification {

    public static Specification<IntegratedSystem> hasField(String fieldName, String value, MatchMode matchMode) {
        return (root, query, criteriaBuilder) -> {
            if (!StringUtils.hasText(value)) return null;

            Path<String> field = root.get(fieldName);

            switch (matchMode) {
                case EXACT:
                    return criteriaBuilder.equal(field, value);
                case CONTAINS:
                    return criteriaBuilder.like(criteriaBuilder.lower(field), "%" + value.toLowerCase() + "%");
                default:
                    return null;
            }
        };
    }

    public static Specification<IntegratedSystem> dateAfter(String fieldName, LocalDate date) {
        return (root, query, criteriaBuilder) -> {
            if (date == null) return null;

            Path<Date> field = root.get(fieldName);
            LocalDateTime startOfDay = date.atStartOfDay();
            return criteriaBuilder.greaterThanOrEqualTo(
                    root.get(fieldName), Timestamp.valueOf(startOfDay)
            );
        };
    }

    public static Specification<IntegratedSystem> dateBefore(String fieldName, LocalDate date) {
        return (root, query, criteriaBuilder) -> {
            if (date == null) return null;

            Path<Date> field = root.get(fieldName);
            LocalDateTime endOfDay = date.atTime(23, 59, 59);
            return criteriaBuilder.lessThanOrEqualTo(
                    root.get(fieldName), Timestamp.valueOf(endOfDay)
            );
        };
    }

    public enum MatchMode {
        EXACT,
        CONTAINS
    }
}