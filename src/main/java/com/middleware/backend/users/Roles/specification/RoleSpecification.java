package com.middleware.backend.users.Roles.specification;

import com.middleware.backend.users.Roles.model.Role;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;

public class RoleSpecification {

    public enum MatchMode {
        EXACT, CONTAINS, STARTS_WITH, ENDS_WITH
    }

    /**
     * Generic method to filter a field in Role entity based on value and match mode.
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
    public static Specification<Role> hasFieldEnum(String fieldName, Enum<?> value) {
        return (root, query, cb) -> {
            if (value == null) return null;
            return cb.equal(root.get(fieldName), value);
        };
    }
}