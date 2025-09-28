package com.middleware.backend.notification.specification;

import com.middleware.backend.notification.model.Receiver;
import org.springframework.data.jpa.domain.Specification;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class ReceiverSpecification {

    public enum MatchMode {
        EXACT,
        CONTAINS
    }

    // Generic string field filter
    public static Specification<Receiver> hasField(String field, String value, MatchMode matchMode) {
        return (root, query, cb) -> {
            if (value == null || value.isEmpty()) {
                return cb.conjunction(); // no filtering if null/empty
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

    // createdAt >= createdAfter
    public static Specification<Receiver> dateAfter(String field, LocalDate date) {
        return (root, query, cb) -> {
            if (date == null) {
                return cb.conjunction();
            }
            LocalDateTime startOfDay = date.atStartOfDay();
            return cb.greaterThanOrEqualTo(root.get(field).as(Timestamp.class), Timestamp.valueOf(startOfDay));
        };
    }

    // createdAt < createdBefore + 1 day
    public static Specification<Receiver> dateBefore(String field, LocalDate date) {
        return (root, query, cb) -> {
            if (date == null) {
                return cb.conjunction();
            }
            LocalDateTime endOfDay = date.plusDays(1).atStartOfDay();
            return cb.lessThan(root.get(field).as(Timestamp.class), Timestamp.valueOf(endOfDay));
        };
    }
}

