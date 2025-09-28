package com.middleware.backend.notification.specification;

import com.middleware.backend.notification.model.NotificationActionsLogs;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

public class NotificationActionsLogsConfigSpecification {

    public enum MatchMode {
        EXACT,
        CONTAINS
    }

    /**
     * Generic string field filter
     */
    public static Specification<NotificationActionsLogs> hasField(String fieldName, String value, MatchMode matchMode) {
        return (root, query, cb) -> {
            if (value == null || value.isEmpty()) {
                return cb.conjunction(); // no filtering
            }
            if (matchMode == MatchMode.CONTAINS) {
                return cb.like(cb.lower(root.get(fieldName)), "%" + value.toLowerCase() + "%");
            } else {
                return cb.equal(cb.lower(root.get(fieldName)), value.toLowerCase());
            }
        };
    }

    /**
     * Filter: created after given date
     */
    public static Specification<NotificationActionsLogs> dateAfter(String fieldName, LocalDate createdAfter) {
        return (root, query, cb) -> {
            if (createdAfter == null) {
                return cb.conjunction();
            }
            Date startDate = Date.from(createdAfter.atStartOfDay(ZoneId.systemDefault()).toInstant());
            return cb.greaterThanOrEqualTo(root.get(fieldName), startDate);
        };
    }

    /**
     * Filter: created before given date
     */
    public static Specification<NotificationActionsLogs> dateBefore(String fieldName, LocalDate createdBefore) {
        return (root, query, cb) -> {
            if (createdBefore == null) {
                return cb.conjunction();
            }
            // end of the day
            Date endDate = Date.from(createdBefore.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant());
            return cb.lessThan(root.get(fieldName), endDate);
        };
    }
}
