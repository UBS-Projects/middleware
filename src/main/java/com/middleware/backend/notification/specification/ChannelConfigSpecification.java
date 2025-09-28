package com.middleware.backend.notification.specification;

import com.middleware.backend.notification.enums.ChannelType;
import com.middleware.backend.notification.model.ChannelConfig;
import org.springframework.data.jpa.domain.Specification;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class ChannelConfigSpecification {

    public enum MatchMode {
        EXACT,
        CONTAINS
    }

    // Match string fields (name, code, config)
    public static Specification<ChannelConfig> hasField(String field, String value, MatchMode matchMode) {
        return (root, query, cb) -> {
            if (value == null || value.isEmpty()) {
                return cb.conjunction();
            }
            switch (matchMode) {
                case EXACT:
                    return cb.equal(root.get(field), value);
                case CONTAINS:
                    return cb.like(cb.lower(root.get(field)), "%" + value.toLowerCase() + "%");
                default:
                    return cb.conjunction();
            }
        };
    }

    // Match the ChannelType enum
    public static Specification<ChannelConfig> hasType(ChannelType type) {
        return (root, query, cb) -> {
            if (type == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("type"), type);
        };
    }

    // Filter records created after a certain date
    public static Specification<ChannelConfig> dateAfter(String field, LocalDate date) {
        return (root, query, cb) -> {
            if (date == null) {
                return cb.conjunction();
            }
            LocalDateTime startOfDay = date.atStartOfDay();
            return cb.greaterThanOrEqualTo(root.get(field).as(Timestamp.class), Timestamp.valueOf(startOfDay));
        };
    }

    // Filter records created before a certain date
    public static Specification<ChannelConfig> dateBefore(String field, LocalDate date) {
        return (root, query, cb) -> {
            if (date == null) {
                return cb.conjunction();
            }
            LocalDateTime endOfDay = date.plusDays(1).atStartOfDay();
            return cb.lessThan(root.get(field).as(Timestamp.class), Timestamp.valueOf(endOfDay));
        };
    }
}
