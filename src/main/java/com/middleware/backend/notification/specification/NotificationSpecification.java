package com.middleware.backend.notification.specification;

import com.middleware.backend.notification.model.NotificationLog;
import org.springframework.data.jpa.domain.Specification;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class NotificationSpecification {

    public enum MatchMode {
        EXACT,
        CONTAINS
    }

    public static Specification<NotificationLog> hasField(String field, String value, MatchMode matchMode) {
        return (root, query, cb) -> {
            if (value == null || value.isEmpty()) {
                return cb.conjunction();
            }

            switch (matchMode) {
                case EXACT:
                    return cb.equal(root.get(field), value);
                case CONTAINS:
                    // For associated entities like group, template, channel, handle nested fields
                    if ("groupName".equals(field)) {
                        return cb.like(cb.lower(root.get("group").get("name")), "%" + value.toLowerCase() + "%");
                    } else if ("templateName".equals(field)) {
                        return cb.like(cb.lower(root.get("template").get("name")), "%" + value.toLowerCase() + "%");
                    } else if ("channelName".equals(field)) {
                        return cb.like(cb.lower(root.get("channel").get("name")), "%" + value.toLowerCase() + "%");
                    } else {
                        return cb.like(cb.lower(root.get(field)), "%" + value.toLowerCase() + "%");
                    }
                default:
                    return cb.conjunction();
            }
        };
    }

    public static Specification<NotificationLog> dateAfter(String field, LocalDate date) {
        return (root, query, cb) -> {
            if (date == null) {
                return cb.conjunction();
            }
            LocalDateTime startOfDay = date.atStartOfDay();
            return cb.greaterThanOrEqualTo(root.get(field).as(Timestamp.class), Timestamp.valueOf(startOfDay));
        };
    }

    public static Specification<NotificationLog> dateBefore(String field, LocalDate date) {
        return (root, query, cb) -> {
            if (date == null) {
                return cb.conjunction();
            }
            LocalDateTime endOfDay = date.plusDays(1).atStartOfDay();
            return cb.lessThan(root.get(field).as(Timestamp.class), Timestamp.valueOf(endOfDay));
        };
    }
}
