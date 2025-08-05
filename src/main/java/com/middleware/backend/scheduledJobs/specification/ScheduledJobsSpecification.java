package com.middleware.backend.scheduledJobs.specification;

import com.middleware.backend.scheduledJobs.enums.MatchMode;
import com.middleware.backend.scheduledJobs.model.ScheduledJobs;
import org.springframework.data.jpa.domain.Specification;

public class ScheduledJobsSpecification {

    public static Specification<ScheduledJobs> hasField(String fieldName, String value, MatchMode mode) {
        return (root, query, cb) -> {
            if (value == null || value.isEmpty()) {
                return null;
            }
            String pattern;
            switch (mode) {
                case STARTS_WITH:
                    pattern = value + "%";
                    break;
                case ENDS_WITH:
                    pattern = "%" + value;
                    break;
                case CONTAINS:
                    pattern = "%" + value + "%";
                    break;
                case EXACT:
                default:
                    pattern = value;
                    break;
            }
            if (mode == MatchMode.EXACT) {
                return cb.equal(root.get(fieldName), value);
            } else {
                return cb.like(root.get(fieldName), pattern);
            }
        };
    }

    public static Specification<ScheduledJobs> hasField(String fieldName, Boolean boolValue) {
        return (root, query, cb) -> {
            if (boolValue == null) {
                return null;
            }
            return cb.equal(root.get(fieldName), boolValue);
        };
    }
}

