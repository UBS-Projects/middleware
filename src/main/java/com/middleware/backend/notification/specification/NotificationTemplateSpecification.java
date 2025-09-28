package com.middleware.backend.notification.specification;

import com.middleware.backend.notification.enums.ChannelType;
import com.middleware.backend.notification.model.NotificationTemplate;
import org.springframework.data.jpa.domain.Specification;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

public class NotificationTemplateSpecification {
    public enum MatchMode {
        EXACT,
        CONTAINS
    }

    public static Specification<NotificationTemplate> hasField(String field, String value, MatchMode matchMode) {
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

    public static Specification<NotificationTemplate> hasType(ChannelType type) {
        return (root, query, cb) -> {
            if (type == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("type"), type);
        };
    }

    public static Specification<NotificationTemplate> dateAfter(String field, LocalDate date) {
        return (root, query, cb) -> {
            if (date == null) {
                return cb.conjunction();
            }
            LocalDateTime startOfDay = date.atStartOfDay();
            return cb.greaterThanOrEqualTo(root.get(field).as(Timestamp.class), Timestamp.valueOf(startOfDay));
        };
    }

    public static Specification<NotificationTemplate> dateBefore(String field, LocalDate date) {
        return (root, query, cb) -> {
            if (date == null) {
                return cb.conjunction();
            }
            LocalDateTime endOfDay = date.plusDays(1).atStartOfDay();
            return cb.lessThan(root.get(field).as(Timestamp.class), Timestamp.valueOf(endOfDay));
        };
    }
}
