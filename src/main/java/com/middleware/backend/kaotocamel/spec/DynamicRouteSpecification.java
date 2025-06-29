package com.middleware.backend.kaotocamel.spec;

import java.time.LocalDateTime;

import org.springframework.data.jpa.domain.Specification;

import com.middleware.backend.kaotocamel.model.DynamicRouteEntity;

public class DynamicRouteSpecification {

    public static Specification<DynamicRouteEntity> hasField(String fieldName, Object value) {
        return (root, query, cb) -> value == null ? null : cb.equal(root.get(fieldName), value);
    }

    public static Specification<DynamicRouteEntity> containsInYaml(String keyword) {
        return (root, query, cb) -> keyword == null ? null : cb.like(root.get("yamlContent"), "%" + keyword + "%");
    }

    public static Specification<DynamicRouteEntity> containsComment(String keyword) {
        return (root, query, cb) -> keyword == null ? null : cb.like(root.get("comment"), "%" + keyword + "%");
    }

    public static Specification<DynamicRouteEntity> createdAfter(LocalDateTime from) {
        return (root, query, cb) -> from == null ? null : cb.greaterThanOrEqualTo(root.get("createdAt"), from);
    }

    public static Specification<DynamicRouteEntity> createdBefore(LocalDateTime to) {
        return (root, query, cb) -> to == null ? null : cb.lessThanOrEqualTo(root.get("createdAt"), to);
    }
}
