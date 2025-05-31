package com.middleware.backend.spec;

import java.util.Map;

import org.springframework.data.jpa.domain.Specification;

import com.middleware.backend.model.ErrorMapping;

public class ErrorMappingSpecification {

    public static Specification<ErrorMapping> filter(Map<String, String> filters) {
        Specification<ErrorMapping> spec = Specification.where(null);

        for (Map.Entry<String, String> entry : filters.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();

            spec = spec.and(getSpecification(key, value));
        }

        return spec;
    }

    private static Specification<ErrorMapping> getSpecification(String key, String value) {
        return (root, query, cb) -> {
            switch (key) {
                case "destinationApiId":
                    return cb.equal(root.get("destinationApi").get("id"), Long.parseLong(value));
                /*case "destinationSystemName":
                    return cb.like(cb.lower(root.get("destinationSystemName")), "%" + value.toLowerCase() + "%");
                    */
                case "matchType":
                    return cb.equal(root.get("matchType"), value);
                    case "rawErrorSubstring":
                    return cb.like(cb.lower(root.get("rawErrorSubstring")), "%" + value.toLowerCase() + "%");
                case "mappedErrorCode":
                    return cb.equal(root.get("mappedErrorCode"), value);
                    case "mappedMessage":
                    return cb.like(cb.lower(root.get("mappedMessage")), "%" + value.toLowerCase() + "%");
                case "errorCategory":
                    return cb.equal(root.get("errorCategory"), value);
                case "language":
                    return cb.equal(root.get("language"), value);
                case "active":
                    return cb.equal(root.get("active"), Boolean.parseBoolean(value));
                case "createdBy":
                    return cb.equal(root.get("createdBy"), Long.parseLong(value));
                    
                default:
                    return null;
            }
        };
    }
}
