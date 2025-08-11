package com.middleware.backend.users.specification;

import com.middleware.backend.users.model.MatchMode;
import com.middleware.backend.users.model.User;
import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.*;

import java.time.LocalDate;
import java.util.Date;

public class UserSpecification {

    public static <T> Specification<User> hasField(String fieldName, Object value, MatchMode mode) {
        return (root, query, cb) -> {
            if (value == null) return null;
            Expression<String> field = root.get(fieldName).as(String.class);

            return switch (mode) {
                case EXACT -> cb.equal(root.get(fieldName), value);
                case STARTS_WITH -> cb.like(cb.lower(field), value.toString().toLowerCase() + "%");
                case ENDS_WITH -> cb.like(cb.lower(field), "%" + value.toString().toLowerCase());
                case CONTAINS -> cb.like(cb.lower(field), "%" + value.toString().toLowerCase() + "%");
            };
        };
    }

    public static Specification<User> dateAfter(String fieldName, LocalDate date) {
        return (root, query, cb) ->
                (date == null) ? null : cb.greaterThanOrEqualTo(root.get(fieldName), java.sql.Timestamp.valueOf(date.atStartOfDay()));
    }

    public static Specification<User> dateBefore(String fieldName, LocalDate date) {
        return (root, query, cb) ->
                (date == null) ? null : cb.lessThanOrEqualTo(root.get(fieldName), java.sql.Timestamp.valueOf(date.plusDays(1).atStartOfDay()));
    }
}


