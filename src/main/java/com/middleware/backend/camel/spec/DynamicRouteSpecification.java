package com.middleware.backend.camel.spec;

import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.data.jpa.domain.Specification;
import com.middleware.backend.camel.model.DynamicRouteEntity;

/**
 * Factory for building JPA Specifications used to filter {@link DynamicRouteEntity}
 * by common fields like routeId, path, description, httpMethod, flags and dates.
 */
public class DynamicRouteSpecification {

    /**
     * Builds a Specification from a map of filter values.
     * Supported keys: routeId, description, path, httpMethod, comment, yamlContains,
     * active, createdAfter, createdBefore, version, defaultVersion.
     *
     * @param filters map of filter names to values
     * @return combined Specification
     */
    public static Specification<DynamicRouteEntity> fromFilters(Map<String, String> filters) {
        Specification<DynamicRouteEntity> spec = Specification.where(null);

        if (filters == null || filters.isEmpty()) {
            return spec;
        }

        if (filters.containsKey("routeId")) {
            spec = spec.and(routeIdContains(filters.get("routeId")));
        }
        if (filters.containsKey("description")) {
            spec = spec.and(descriptionContains(filters.get("description")));
        }
        if (filters.containsKey("path")) {
            spec = spec.and(pathContains(filters.get("path")));
        }
        if (filters.containsKey("httpMethod")) {
            spec = spec.and(httpMethodContains(filters.get("httpMethod")));
        }
        if (filters.containsKey("comment")) {
            spec = spec.and(containsComment(filters.get("comment")));
        }
        if (filters.containsKey("yamlContains")) {
            spec = spec.and(containsInYaml(filters.get("yamlContains")));
        }
        if (filters.containsKey("active")) {
            Boolean active = Boolean.valueOf(filters.get("active"));
            spec = spec.and(hasActiveStatus(active));
        }
        if (filters.containsKey("createdAfter")) {
            LocalDateTime after = LocalDateTime.parse(filters.get("createdAfter"));
            spec = spec.and(createdAfter(after));
        }
        if (filters.containsKey("createdBefore")) {
            LocalDateTime before = LocalDateTime.parse(filters.get("createdBefore"));
            spec = spec.and(createdBefore(before));
        }
        if (filters.containsKey("version")) {
            Integer version = Integer.valueOf(filters.get("version"));
            spec = spec.and(hasVersion(version));
        }
        if (filters.containsKey("defaultVersion")) {
            Boolean defaultVersion = Boolean.valueOf(filters.get("defaultVersion"));
            spec = spec.and(isDefaultVersion(defaultVersion));
        }

        return spec;
    }
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