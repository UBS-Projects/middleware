package com.middleware.backend.kaotocamel.spec;

import java.time.LocalDateTime;
import org.springframework.data.jpa.domain.Specification;
import com.middleware.backend.kaotocamel.model.DynamicRouteEntity;

/**
 * Factory for building JPA Specifications used to filter {@link DynamicRouteEntity}
 * by common fields like routeId, path, description, httpMethod, flags and dates.
 */
public class DynamicRouteSpecification {

    /**
     * Generic field specification with exact match.
     */
    public static Specification<DynamicRouteEntity> hasField(String fieldName, Object value) {
        return (root, query, criteriaBuilder) -> {
            if (value == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get(fieldName), value);
        };
    }

    /**
     * Path specification with LIKE search (partial matching).
     */
    public static Specification<DynamicRouteEntity> pathContains(String path) {
        return (root, query, criteriaBuilder) -> {
            if (path == null || path.trim().isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("path")),
                    "%" + path.toLowerCase().trim() + "%"
            );
        };
    }

    /**
     * Route ID specification with LIKE search (partial matching).
     */
    public static Specification<DynamicRouteEntity> routeIdContains(String routeId) {
        return (root, query, criteriaBuilder) -> {
            if (routeId == null || routeId.trim().isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("routeId")),
                    "%" + routeId.toLowerCase().trim() + "%"
            );
        };
    }

    /**
     * Description specification with LIKE search (partial matching).
     */
    public static Specification<DynamicRouteEntity> descriptionContains(String description) {
        return (root, query, criteriaBuilder) -> {
            if (description == null || description.trim().isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("description")),
                    "%" + description.toLowerCase().trim() + "%"
            );
        };
    }

    /**
     * HTTP Method specification with LIKE search (partial matching).
     */
    public static Specification<DynamicRouteEntity> httpMethodContains(String httpMethod) {
        return (root, query, criteriaBuilder) -> {
            if (httpMethod == null || httpMethod.trim().isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("httpMethod")),
                    "%" + httpMethod.toLowerCase().trim() + "%"
            );
        };
    }

    /**
     * Comment contains specification.
     */
    public static Specification<DynamicRouteEntity> containsComment(String comment) {
        return (root, query, criteriaBuilder) -> {
            if (comment == null || comment.trim().isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("comment")),
                    "%" + comment.toLowerCase().trim() + "%"
            );
        };
    }

    /**
     * YAML content contains specification.
     */
    public static Specification<DynamicRouteEntity> containsInYaml(String yamlContains) {
        return (root, query, criteriaBuilder) -> {
            if (yamlContains == null || yamlContains.trim().isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("yamlContent")),
                    "%" + yamlContains.toLowerCase().trim() + "%"
            );
        };
    }

    /**
     * Created after specification.
     */
    public static Specification<DynamicRouteEntity> createdAfter(LocalDateTime date) {
        return (root, query, criteriaBuilder) -> {
            if (date == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), date);
        };
    }

    /**
     * Created before specification.
     */
    public static Specification<DynamicRouteEntity> createdBefore(LocalDateTime date) {
        return (root, query, criteriaBuilder) -> {
            if (date == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.lessThanOrEqualTo(root.get("createdAt"), date);
        };
    }

    /**
     * Active status specification.
     */
    public static Specification<DynamicRouteEntity> hasActiveStatus(Boolean active) {
        return (root, query, criteriaBuilder) -> {
            if (active == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("active"), active);
        };
    }

    /**
     * Version specification.
     */
    public static Specification<DynamicRouteEntity> hasVersion(Integer version) {
        return (root, query, criteriaBuilder) -> {
            if (version == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("version"), version);
        };
    }

    /**
     * Default version specification.
     */
    public static Specification<DynamicRouteEntity> isDefaultVersion(Boolean defaultVersion) {
        return (root, query, criteriaBuilder) -> {
            if (defaultVersion == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("defaultVersion"), defaultVersion);
        };
    }
}