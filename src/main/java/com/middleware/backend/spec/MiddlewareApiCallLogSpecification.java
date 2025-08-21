package com.middleware.backend.spec;

import com.middleware.backend.logging.model.MiddlewareApiCallLog;
import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.Predicate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MiddlewareApiCallLogSpecification {

    public static Specification<MiddlewareApiCallLog> fromFilters(Map<String, String> filters) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            filters.forEach((key, value) -> {
                if (value == null || value.isBlank()) return;

                try {
                    switch (key) {
                        case "durationMsMin" -> predicates.add(cb.ge(root.get("durationMs"), Long.parseLong(value)));
                        case "durationMsMax" -> predicates.add(cb.le(root.get("durationMs"), Long.parseLong(value)));
                        case "retryCountMin" -> predicates.add(cb.ge(root.get("retryCount"), Integer.parseInt(value)));
                        case "retryCountMax" -> predicates.add(cb.le(root.get("retryCount"), Integer.parseInt(value)));

                        case "responseCode" -> predicates.add(cb.equal(root.get(key), Integer.parseInt(value)));
                        case "throttlingApplied" -> predicates.add(cb.equal(root.get(key), Boolean.parseBoolean(value)));

                        case "receivedAtFrom" -> {
                            LocalDate date = LocalDate.parse(value); // parse yyyy-MM-dd
                            predicates.add(cb.greaterThanOrEqualTo(root.get("receivedAt"), date.atStartOfDay()));
                        }
                        case "receivedAtTo" -> {
                            LocalDate date = LocalDate.parse(value);
                            predicates.add(cb.lessThanOrEqualTo(root.get("receivedAt"), date.atTime(23, 59, 59)));
                        }

                        case "completedAtFrom" ->
                                predicates.add(cb.greaterThanOrEqualTo(root.get("completedAt"), LocalDateTime.parse(value)));
                        case "completedAtTo" ->
                                predicates.add(cb.lessThanOrEqualTo(root.get("completedAt"), LocalDateTime.parse(value)));

                        case "id", "apiEndpointId", "workflowId", "apiKeyId", "userId" ->
                                predicates.add(cb.equal(root.get(key), Long.parseLong(value)));

                        case "responseCodeGt" -> predicates.add(cb.gt(root.get("responseCode"), Integer.parseInt(value)));
                        case "responseCodeLt" -> predicates.add(cb.lt(root.get("responseCode"), Integer.parseInt(value)));
                        case "responseCodeMin" -> predicates.add(cb.ge(root.get("responseCode"), Integer.parseInt(value)));
                        case "responseCodeMax" -> predicates.add(cb.le(root.get("responseCode"), Integer.parseInt(value)));

                        default -> predicates.add(cb.like(cb.lower(root.get(key)), "%" + value.toLowerCase() + "%"));
                    }
                } catch (Exception ignored) {
                }
            });

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}

