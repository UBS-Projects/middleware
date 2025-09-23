package com.middleware.backend.errormapping.spec;

  import com.middleware.backend.errormapping.model.ErrorCategory;
  import com.middleware.backend.errormapping.model.ErrorMapping;
  import com.middleware.backend.model.SourceSystem;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

import java.util.Map;

public class ErrorMappingSpecification {

    public static Specification<ErrorMapping> filter(Map<String, String> filters) {
        Specification<ErrorMapping> spec = Specification.where(null);

        if (filters == null) {
            return spec;
        }

        for (Map.Entry<String, String> entry : filters.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();

            if (value != null && !value.trim().isEmpty()) {
                spec = spec.and(getSpecification(key, value));
            }
        }

        return spec;
    }

    private static Specification<ErrorMapping> getSpecification(String key, String value) {
        return (root, query, cb) -> {
            switch (key) {
                case "routeId":
                    return cb.like(cb.lower(root.get("routeId")), "%" + value.toLowerCase() + "%");
                case "routePath":
                    return cb.like(cb.lower(root.get("routePath")), "%" + value.toLowerCase() + "%");

                case "sourceSystem":
                    if (query.getResultType() != Long.class && query.getResultType() != long.class) {
                        Join<ErrorMapping, SourceSystem> sourceSystemJoin = root.join("sourceSystem", JoinType.LEFT);
                        return cb.like(cb.lower(sourceSystemJoin.get("name")), "%" + value.toLowerCase() + "%");
                    }
                    return null;

                case "sourceSystemId":
                    Join<ErrorMapping, SourceSystem> sourceSystemJoinById = root.join("sourceSystem");
                    return cb.equal(sourceSystemJoinById.get("id"), Long.parseLong(value));

                case "matchType":
                    return cb.equal(root.get("matchType"), ErrorMapping.MatchType.valueOf(value.toUpperCase()));
                case "rawErrorSubstring":
                    return cb.like(cb.lower(root.get("rawErrorSubstring")), "%" + value.toLowerCase() + "%");
                case "mappedErrorCode":
                    return cb.like(cb.lower(root.get("mappedErrorCode")), "%" + value.toLowerCase() + "%");
                case "mappedMessage":
                    return cb.like(cb.lower(root.get("mappedMessage")), "%" + value.toLowerCase() + "%");

                case "errorCategory":
                    if (query.getResultType() != Long.class && query.getResultType() != long.class) {
                        Join<ErrorMapping, ErrorCategory> categoryJoin = root.join("errorCategory", JoinType.LEFT);
                        return cb.like(cb.lower(categoryJoin.get("name")), "%" + value.toLowerCase() + "%");
                    }
                    return null;

                case "errorCategoryId":
                    Join<ErrorMapping, ErrorCategory> categoryJoinById = root.join("errorCategory");
                    return cb.equal(categoryJoinById.get("id"), Long.parseLong(value));

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