package com.middleware.backend.users.specification;

import com.middleware.backend.users.Roles.model.Role;
import com.middleware.backend.users.model.MatchMode;
import com.middleware.backend.users.model.User;
import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.*;

import java.time.LocalDate;
import java.util.Date;

/**
 * JPA Specifications to filter {@link com.middleware.backend.users.model.User}.
 */
public class UserSpecification {

    /**
     * Matches a field using a given text match mode.
     * For non-string values, the value is converted to string and compared in lowercase when using like operations.
     */
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

    /**
     * Filters users by role name using an inner join on roles.
     * Returns conjunction when the role name is null/empty (no-op filter).
     */
    public static Specification<User> hasRole(String roleName) {
        return (root, query, cb) -> {
            if (roleName == null || roleName.isEmpty()) {
                return cb.conjunction();
            }
            Join<User, Role> roleJoin = root.join("roles", JoinType.INNER);
            return cb.equal(roleJoin.get("roleName"), roleName);
        };
    }




    /**
     * Filters by a timestamp field with lower bound inclusive (start of day).
     */
    public static Specification<User> dateAfter(String fieldName, LocalDate date) {
        return (root, query, cb) ->
                (date == null) ? null : cb.greaterThanOrEqualTo(root.get(fieldName), java.sql.Timestamp.valueOf(date.atStartOfDay()));
    }

    /**
     * Filters by a timestamp field with upper bound inclusive (end of day).
     */
    public static Specification<User> dateBefore(String fieldName, LocalDate date) {
        return (root, query, cb) ->
                (date == null) ? null : cb.lessThanOrEqualTo(root.get(fieldName), java.sql.Timestamp.valueOf(date.plusDays(1).atStartOfDay()));
    }

    /**
     * Filters users by {@link Role.RoleType}; enforces distinct to avoid duplicates.
     */
    public static Specification<User> hasRoleType(Role.RoleType roleType) {
        return (root, query, cb) -> {
            if (roleType == null) {
                return cb.conjunction();
            }
            root.fetch("roles", JoinType.LEFT); // optional fetch optimization
            query.distinct(true);               // <--- important!
            Join<User, Role> roleJoin = root.join("roles", JoinType.INNER);
            return cb.equal(roleJoin.get("roleType"), roleType);
        };
    }
    /**
     * Excludes a specific email from results (useful to hide current user).
     */
    public static Specification<User> emailNotEqual(String email) {
        return (root, query, cb) -> {
            if (email == null) return cb.conjunction();
            return cb.notEqual(root.get("email"), email);
        };
    }


}


