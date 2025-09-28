package com.middleware.backend.system_settings.specificaion;

import com.middleware.backend.system_settings.model.Config;
import com.middleware.backend.system_settings.model.ConfigType;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class ConfigSpecification {
    public enum MatchMode {
        EXACT,
        CONTAINS,
        STARTS_WITH,
        ENDS_WITH
    }

    /**
     * Generic filter for String fields.
     */
    public static Specification<Config> hasField(String fieldName, String value, MatchMode mode) {
        return (root, query, builder) -> {
            if (!StringUtils.hasText(value)) {
                return builder.conjunction();
            }
            Path<String> fieldPath = root.get(fieldName);
            Expression<String> lowerField = builder.lower(fieldPath);
            String lowerValue = value.toLowerCase();

            return switch (mode) {
                case EXACT -> builder.equal(lowerField, lowerValue);
                case CONTAINS -> builder.like(lowerField, "%" + lowerValue + "%");
                case STARTS_WITH -> builder.like(lowerField, lowerValue + "%");
                case ENDS_WITH -> builder.like(lowerField, "%" + lowerValue);
            };
        };
    }

    /**
     * Filter by enum type (ConfigType).
     */
    public static Specification<Config> hasType(ConfigType type) {
        return (root, query, builder) -> {
            if (type == null) {
                return builder.conjunction();
            }
            return builder.equal(root.get("type"), type);
        };
    }

    /**
     * Created after given datetime.
     */
    public static Specification<Config> dateAfter(String fieldName, LocalDate date) {
        return (root, query, builder) -> {
            if (date == null) return builder.conjunction();
            return builder.greaterThanOrEqualTo(root.get(fieldName), date.atStartOfDay());
        };
    }

    public static Specification<Config> dateBefore(String fieldName, LocalDate date) {
        return (root, query, builder) -> {
            if (date == null) return builder.conjunction();
            return builder.lessThanOrEqualTo(root.get(fieldName), date.atTime(LocalTime.MAX));
        };
    }
}
