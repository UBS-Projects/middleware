package com.middleware.backend.kaotocamel.spec;

import com.middleware.backend.kaotocamel.model.DynamicRouteAudit;
import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import java.time.LocalDate;
import java.time.LocalDateTime;
public class DynamicRouteLogsSpecification {
    public enum MatchMode {
        EXACT, CONTAINS
    }

    public static <T> Specification<DynamicRouteAudit> hasField(String fieldName, String value, MatchMode matchMode) {
        return (Root<DynamicRouteAudit> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> {
            if (value == null || value.trim().isEmpty()) {
                return cb.conjunction();
            }
            if (matchMode == MatchMode.EXACT) {
                return cb.equal(root.get(fieldName), value);
            } else { // CONTAINS
                return cb.like(cb.lower(root.get(fieldName)), "%" + value.toLowerCase() + "%");
            }
        };
    }

    public static <T> Specification<DynamicRouteAudit> createdBetween(LocalDate startDate, LocalDate endDate) {
        return (Root<DynamicRouteAudit> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> {
            if (startDate == null && endDate == null) {
                return cb.conjunction();
            }

            LocalDateTime startDateTime = null;
            LocalDateTime endDateTime = null;

            if (startDate != null) {
                startDateTime = startDate.atStartOfDay();
            }
            if (endDate != null) {
                // include the whole end day
                endDateTime = endDate.atTime(23, 59, 59);
            }

            if (startDateTime != null && endDateTime != null) {
                return cb.between(root.get("timestamp"), startDateTime, endDateTime);
            } else if (startDateTime != null) {
                return cb.greaterThanOrEqualTo(root.get("timestamp"), startDateTime);
            } else {
                return cb.lessThanOrEqualTo(root.get("timestamp"), endDateTime);
            }
        };
    }
}