package com.middleware.backend.kaotocamel.spec;

import com.middleware.backend.kaotocamel.model.IntegrationMapping;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Comprehensive specification for filtering IntegrationMapping entities
 * Supports filtering on all fields with powerful search capabilities
 */
public class IntegrationMappingSpecification {

    /**
     * Filter by middleware API name (partial match, case-insensitive)
     */
    public static Specification<IntegrationMapping> middlewareApiNameContains(String middlewareApiName) {
        return (root, query, criteriaBuilder) -> {
            if (middlewareApiName == null || middlewareApiName.trim().isEmpty()) {
                return null;
            }
            return criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("middlewareApiName")),
                    "%" + middlewareApiName.toLowerCase().trim() + "%"
            );
        };
    }

    /**
     * Filter by exact middleware API name match
     */
    public static Specification<IntegrationMapping> middlewareApiNameEquals(String middlewareApiName) {
        return (root, query, criteriaBuilder) -> {
            if (middlewareApiName == null || middlewareApiName.trim().isEmpty()) {
                return null;
            }
            return criteriaBuilder.equal(
                    criteriaBuilder.lower(root.get("middlewareApiName")),
                    middlewareApiName.toLowerCase().trim()
            );
        };
    }

    /**
     * Filter by integrated API ID
     */
    public static Specification<IntegrationMapping> integratedApiIdEquals(Long integratedApiId) {
        return (root, query, criteriaBuilder) -> {
            if (integratedApiId == null) {
                return null;
            }
            return criteriaBuilder.equal(root.get("integratedApiId"), integratedApiId);
        };
    }

    /**
     * Filter by integrated API code
     */
    public static Specification<IntegrationMapping> integratedApiCodeContains(String integratedApiCode) {
        return (root, query, criteriaBuilder) -> {
            if (integratedApiCode == null || integratedApiCode.trim().isEmpty()) {
                return null;
            }
            return criteriaBuilder.like(
                    criteriaBuilder.lower(root.join("integratedApi").get("code")),
                    "%" + integratedApiCode.toLowerCase().trim() + "%"
            );
        };
    }

    /**
     * Filter by mapping type
     */
    public static Specification<IntegrationMapping> mappingTypeEquals(String mappingType) {
        return (root, query, criteriaBuilder) -> {
            if (mappingType == null || mappingType.trim().isEmpty()) {
                return null;
            }
            try {
                IntegrationMapping.MappingType type = IntegrationMapping.MappingType.valueOf(mappingType.toUpperCase().trim());
                return criteriaBuilder.equal(root.get("mappingType"), type);
            } catch (IllegalArgumentException e) {
                return criteriaBuilder.disjunction(); // Return false condition
            }
        };
    }

    /**
     * Filter by multiple mapping types
     */
    public static Specification<IntegrationMapping> mappingTypeIn(List<String> mappingTypes) {
        return (root, query, criteriaBuilder) -> {
            if (mappingTypes == null || mappingTypes.isEmpty()) {
                return null;
            }

            List<IntegrationMapping.MappingType> types = new ArrayList<>();
            for (String type : mappingTypes) {
                try {
                    types.add(IntegrationMapping.MappingType.valueOf(type.toUpperCase().trim()));
                } catch (IllegalArgumentException e) {
                    // Skip invalid types
                }
            }

            if (types.isEmpty()) {
                return criteriaBuilder.disjunction();
            }

            return root.get("mappingType").in(types);
        };
    }

    /**
     * Filter by data (partial match, case-insensitive)
     */
    public static Specification<IntegrationMapping> dataContains(String data) {
        return (root, query, criteriaBuilder) -> {
            if (data == null || data.trim().isEmpty()) {
                return null;
            }
            return criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("data")),
                    "%" + data.toLowerCase().trim() + "%"
            );
        };
    }

    /**
     * Filter by external key (partial match, case-insensitive)
     */
    public static Specification<IntegrationMapping> externalKeyContains(String externalKey) {
        return (root, query, criteriaBuilder) -> {
            if (externalKey == null || externalKey.trim().isEmpty()) {
                return null;
            }
            return criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("externalKey")),
                    "%" + externalKey.toLowerCase().trim() + "%"
            );
        };
    }

    /**
     * Filter by exact external key match
     */
    public static Specification<IntegrationMapping> externalKeyEquals(String externalKey) {
        return (root, query, criteriaBuilder) -> {
            if (externalKey == null || externalKey.trim().isEmpty()) {
                return null;
            }
            return criteriaBuilder.equal(
                    criteriaBuilder.lower(root.get("externalKey")),
                    externalKey.toLowerCase().trim()
            );
        };
    }

    /**
     * Filter by attribute (partial match, case-insensitive)
     */
    public static Specification<IntegrationMapping> attributeContains(String attribute) {
        return (root, query, criteriaBuilder) -> {
            if (attribute == null || attribute.trim().isEmpty()) {
                return null;
            }
            return criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("attribute")),
                    "%" + attribute.toLowerCase().trim() + "%"
            );
        };
    }

    /**
     * Filter by active status
     */
    public static Specification<IntegrationMapping> hasActiveStatus(Boolean isActive) {
        return (root, query, criteriaBuilder) -> {
            if (isActive == null) {
                return null;
            }
            return criteriaBuilder.equal(root.get("isActive"), isActive);
        };
    }

    /**
     * Filter by notes (partial match, case-insensitive)
     */
    public static Specification<IntegrationMapping> notesContains(String notes) {
        return (root, query, criteriaBuilder) -> {
            if (notes == null || notes.trim().isEmpty()) {
                return null;
            }
            return criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("notes")),
                    "%" + notes.toLowerCase().trim() + "%"
            );
        };
    }

    /**
     * Filter by creation date range
     */
    public static Specification<IntegrationMapping> createdBetween(LocalDateTime startDate, LocalDateTime endDate) {
        return (root, query, criteriaBuilder) -> {
            if (startDate == null && endDate == null) {
                return null;
            }

            if (startDate != null && endDate != null) {
                return criteriaBuilder.between(root.get("createdAt"), startDate, endDate);
            } else if (startDate != null) {
                return criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), startDate);
            } else {
                return criteriaBuilder.lessThanOrEqualTo(root.get("createdAt"), endDate);
            }
        };
    }

    /**
     * Filter by creation date after
     */
    public static Specification<IntegrationMapping> createdAfter(LocalDateTime createdAfter) {
        return (root, query, criteriaBuilder) -> {
            if (createdAfter == null) {
                return null;
            }
            return criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), createdAfter);
        };
    }

    /**
     * Filter by creation date before
     */
    public static Specification<IntegrationMapping> createdBefore(LocalDateTime createdBefore) {
        return (root, query, criteriaBuilder) -> {
            if (createdBefore == null) {
                return null;
            }
            return criteriaBuilder.lessThanOrEqualTo(root.get("createdAt"), createdBefore);
        };
    }

    /**
     * Filter by update date range
     */
    public static Specification<IntegrationMapping> updatedBetween(LocalDateTime startDate, LocalDateTime endDate) {
        return (root, query, criteriaBuilder) -> {
            if (startDate == null && endDate == null) {
                return null;
            }

            if (startDate != null && endDate != null) {
                return criteriaBuilder.between(root.get("updatedAt"), startDate, endDate);
            } else if (startDate != null) {
                return criteriaBuilder.greaterThanOrEqualTo(root.get("updatedAt"), startDate);
            } else {
                return criteriaBuilder.lessThanOrEqualTo(root.get("updatedAt"), endDate);
            }
        };
    }

    /**
     * Global search across multiple text fields
     */
    public static Specification<IntegrationMapping> globalSearch(String searchTerm) {
        return (root, query, criteriaBuilder) -> {
            if (searchTerm == null || searchTerm.trim().isEmpty()) {
                return null;
            }

            String searchPattern = "%" + searchTerm.toLowerCase().trim() + "%";

            return criteriaBuilder.or(
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("middlewareApiName")), searchPattern),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("data")), searchPattern),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("externalKey")), searchPattern),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("attribute")), searchPattern),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("notes")), searchPattern),
                    criteriaBuilder.like(criteriaBuilder.lower(root.join("integratedApi").get("code")), searchPattern),
                    criteriaBuilder.like(criteriaBuilder.lower(root.join("integratedApi").get("name")), searchPattern)
            );
        };
    }

    /**
     * Filter by ID range
     */
    public static Specification<IntegrationMapping> idBetween(Long minId, Long maxId) {
        return (root, query, criteriaBuilder) -> {
            if (minId == null && maxId == null) {
                return null;
            }

            if (minId != null && maxId != null) {
                return criteriaBuilder.between(root.get("id"), minId, maxId);
            } else if (minId != null) {
                return criteriaBuilder.greaterThanOrEqualTo(root.get("id"), minId);
            } else {
                return criteriaBuilder.lessThanOrEqualTo(root.get("id"), maxId);
            }
        };
    }

    /**
     * Complex combined specification builder
     */
    public static Specification<IntegrationMapping> buildSpecification(
            String middlewareApiName,
            Long integratedApiId,
            String integratedApiCode,
            String mappingType,
            String data,
            String externalKey,
            String attribute,
            Boolean isActive,
            String notes,
            LocalDateTime createdAfter,
            LocalDateTime createdBefore,
            LocalDateTime updatedAfter,
            LocalDateTime updatedBefore,
            String globalSearch,
            Long minId,
            Long maxId
    ) {
        return Specification.where(middlewareApiNameContains(middlewareApiName))
                .and(integratedApiIdEquals(integratedApiId))
                .and(integratedApiCodeContains(integratedApiCode))
                .and(mappingTypeEquals(mappingType))
                .and(dataContains(data))
                .and(externalKeyContains(externalKey))
                .and(attributeContains(attribute))
                .and(hasActiveStatus(isActive))
                .and(notesContains(notes))
                .and(createdAfter(createdAfter))
                .and(createdBefore(createdBefore))
                .and(updatedBetween(updatedAfter, updatedBefore))
                .and(globalSearch(globalSearch))
                .and(idBetween(minId, maxId));
    }
}