package com.middleware.backend.notification.specification;
import com.middleware.backend.notification.model.NotificationGroup;
import org.springframework.data.jpa.domain.Specification;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class NotificationGroupSpecification {

    public enum MatchMode {
        EXACT,
        CONTAINS
    }

    public static Specification<NotificationGroup> hasField(String field, String value, MatchMode matchMode) {
        return (root, query, cb) -> {
            if (value == null || value.isEmpty()) {
                return cb.conjunction(); // ignore filter if value is null/empty
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

    public static Specification<NotificationGroup> dateAfter(String field, LocalDate date) {
        return (root, query, cb) -> {
            if (date == null) {
                return cb.conjunction();
            }
            LocalDateTime startOfDay = date.atStartOfDay();
            return cb.greaterThanOrEqualTo(root.get(field).as(Timestamp.class), Timestamp.valueOf(startOfDay));
        };
    }

    public static Specification<NotificationGroup> dateBefore(String field, LocalDate date) {
        return (root, query, cb) -> {
            if (date == null) {
                return cb.conjunction();
            }
            LocalDateTime endOfDay = date.plusDays(1).atStartOfDay();
            return cb.lessThan(root.get(field).as(Timestamp.class), Timestamp.valueOf(endOfDay));
        };
    }
}
