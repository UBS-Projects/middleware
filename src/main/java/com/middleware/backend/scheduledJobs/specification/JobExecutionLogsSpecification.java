package com.middleware.backend.scheduledJobs.specification;

import com.middleware.backend.scheduledJobs.model.JobExecutionLogs;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

/**
 * Factory for {@link Specification} filters against {@link JobExecutionLogs}.
 */
public class JobExecutionLogsSpecification {

    public enum MatchMode { EXACT, CONTAINS }

    /**
     * Builds a generic equals/contains filter for the given field. Supports nested paths like "job.jobName".
     */
    public static <T> Specification<JobExecutionLogs> hasField(String field, T value, MatchMode matchMode) {
        return (root, query, cb) -> {
            if (value == null) return null;

            // support nested fields like "job.jobName"
            if (field.contains(".")) {
                String[] parts = field.split("\\.");
                var path = root.get(parts[0]).get(parts[1]);

                return switch (matchMode) {
                    case EXACT -> cb.equal(path, value);
                    case CONTAINS -> (value instanceof String str)
                            ? cb.like(cb.lower(path.as(String.class)), "%" + str.toLowerCase() + "%")
                            : null;
                };
            }

            // simple field
            return switch (matchMode) {
                case EXACT -> cb.equal(root.get(field), value);
                case CONTAINS -> (value instanceof String str)
                        ? cb.like(cb.lower(root.get(field).as(String.class)), "%" + str.toLowerCase() + "%")
                        : null;
            };
        };
    }

    /**
     * Filters logs by startTime between inclusive day boundaries from LocalDate inputs.
     */
    public static Specification<JobExecutionLogs> startedBetween(LocalDate startAfter, LocalDate startBefore) {
        return (root, query, cb) -> {
            if (startAfter == null && startBefore == null) return null;

            Date afterDate = startAfter != null ? Date.from(startAfter.atStartOfDay(ZoneId.systemDefault()).toInstant()) : null;
            Date beforeDate = startBefore != null ? Date.from(startBefore.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()) : null;

            if (afterDate != null && beforeDate != null) {
                return cb.between(root.get("startTime"), afterDate, beforeDate);
            } else if (afterDate != null) {
                return cb.greaterThanOrEqualTo(root.get("startTime"), afterDate);
            } else {
                return cb.lessThan(root.get("startTime"), beforeDate);
            }
        };
    }
}
