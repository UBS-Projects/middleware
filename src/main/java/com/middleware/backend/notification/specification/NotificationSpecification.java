package com.middleware.backend.notification.specification;

import com.middleware.backend.notification.model.NotificationLog;
import org.springframework.data.jpa.domain.Specification;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Provides JPA Specifications for dynamically filtering NotificationLog entities.
 * Supports filtering by string fields (including associated entity names) and date ranges.
 */
public class NotificationSpecification {

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
     * Returns a Specification to filter NotificationLog based on a string field.
     * Supports nested fields for associated entities: groupName, templateName, channelName.
     *
     * @param field the name of the field to filter (e.g., "groupName", "templateName", "channelName", or other fields)
     * @param value the value to match
     * @param matchMode how the value should be matched (EXACT or CONTAINS)
     * @return a Specification for filtering NotificationLog
     */
    public static Specification<NotificationLog> hasField(String field, String value, MatchMode matchMode) {
        return (root, query, cb) -> {
            if (value == null || value.isEmpty()) {
                return cb.conjunction(); // ignore filter if value is null/empty
            }

            String lowerValue = "%" + value.toLowerCase() + "%";

            switch (matchMode) {
                case EXACT:
                    return cb.equal(cb.lower(root.get(field)), value.toLowerCase());
                case CONTAINS:
                    // All fields are direct columns (not nested entities)
                    return cb.like(cb.lower(root.get(field)), lowerValue);
                default:
                    return cb.conjunction();
            }
        };
    }


    /**
     * Returns a Specification to filter NotificationLog records created on or after a given date.
     *
     * @param field the name of the timestamp field to filter (e.g., "sentAt", "createdAt")
     * @param date the start date (inclusive)
     * @return a Specification for filtering NotificationLog records after the given date
     */
    public static Specification<NotificationLog> dateAfter(String field, LocalDate date) {
        return (root, query, cb) -> {
            if (date == null) {
                return cb.conjunction();
            }
            LocalDateTime startOfDay = date.atStartOfDay();
            return cb.greaterThanOrEqualTo(root.get(field).as(Timestamp.class), Timestamp.valueOf(startOfDay));
        };
    }

    /**
     * Returns a Specification to filter NotificationLog records created before a given date.
     *
     * @param field the name of the timestamp field to filter (e.g., "sentAt", "createdAt")
     * @param date the end date (exclusive)
     * @return a Specification for filtering NotificationLog records before the given date
     */
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

