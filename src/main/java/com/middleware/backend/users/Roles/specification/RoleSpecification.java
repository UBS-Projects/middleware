package com.middleware.backend.users.Roles.specification;

import com.middleware.backend.users.Roles.model.Role;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;

/**
 * Utility class providing reusable {@link org.springframework.data.jpa.domain.Specification} builders
 * for filtering {@link com.middleware.backend.users.Roles.model.Role} entities.
 */
public class RoleSpecification {

    /**
     * Modes supported when matching String fields.
     */
    public enum MatchMode {
        EXACT, CONTAINS, STARTS_WITH, ENDS_WITH
    }

    /**
     * Build a {@link Specification} that filters a String field with a chosen match mode.
     *
     * @param fieldName the Role field name to filter (must be a String field)
     * @param value     the value to match; when null/empty, returns a no-op predicate
     * @param matchMode the {@link MatchMode} to apply (exact, contains, starts, ends)
     * @return {@link Specification} to be composed with other specs
     */
    public static Specification<Role> hasField(String fieldName, String value, MatchMode matchMode) {
        return (Root<Role> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> {
            if (value == null || value.isEmpty()) {
                return cb.conjunction(); // No filtering
            }

            Path<String> path = root.get(fieldName);

            switch (matchMode) {
                case EXACT:
                    return cb.equal(path, value);
                case CONTAINS:
                    return cb.like(cb.lower(path), "%" + value.toLowerCase() + "%");
                case STARTS_WITH:
                    return cb.like(cb.lower(path), value.toLowerCase() + "%");
                case ENDS_WITH:
                    return cb.like(cb.lower(path), "%" + value.toLowerCase());
                default:
                    return cb.conjunction();
            }
        };
    }
    /**
     * Build a {@link Specification} that filters an enum-valued field by equality.
     *
     * @param fieldName the Role field name to filter (enum type)
     * @param value     enum constant to match; null yields no predicate
     * @return {@link Specification} checking equality, or null when value is null
     */
    public static Specification<Role> hasFieldEnum(String fieldName, Enum<?> value) {
        return (root, query, cb) -> {
            if (value == null) return null;
            return cb.equal(root.get(fieldName), value);
        };
    }
}