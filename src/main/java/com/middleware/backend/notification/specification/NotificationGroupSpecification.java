package com.middleware.backend.notification.specification;
import com.middleware.backend.notification.model.NotificationGroup;
import org.springframework.data.jpa.domain.Specification;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Provides JPA Specifications for dynamically filtering NotificationGroup entities.
 * Specifications can be combined to build flexible queries.
 */
public class NotificationGroupSpecification {

    /**
     * Defines how string fields should be matched in queries.
     */
    public enum MatchMode {
        /**
         * Match the field value exactly.
         */
        EXACT,

        /**
         * Perform a case-insensitive partial match (like '%value%').
         */
        CONTAINS
    }

    /**
     * Returns a Specification to filter NotificationGroup based on a string field.
     *
     * @param field the name of the field to filter (e.g., "name", "code", "description")
     * @param value the value to match
     * @param matchMode how the value should be matched (EXACT or CONTAINS)
     * @return a Specification for filtering NotificationGroup
     */
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

    /**
     * Returns a Specification to filter NotificationGroup records created on or after a given date.
     *
     * @param field the name of the timestamp field to filter (e.g., "createdAt", "updatedAt")
     * @param date the start date (inclusive)
     * @return a Specification for filtering NotificationGroup records after the given date
     */
    public static Specification<NotificationGroup> dateAfter(String field, LocalDate date) {
        return (root, query, cb) -> {
            if (date == null) {
                return cb.conjunction();
            }
            LocalDateTime startOfDay = date.atStartOfDay();
            return cb.greaterThanOrEqualTo(root.get(field).as(Timestamp.class), Timestamp.valueOf(startOfDay));
        };
    }

    /**
     * Returns a Specification to filter NotificationGroup records created before a given date.
     *
     * @param field the name of the timestamp field to filter (e.g., "createdAt", "updatedAt")
     * @param date the end date (exclusive)
     * @return a Specification for filtering NotificationGroup records before the given date
     */
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

