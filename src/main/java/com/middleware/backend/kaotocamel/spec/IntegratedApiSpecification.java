//package com.middleware.backend.kaotocamel.spec;
//
//import com.middleware.backend.kaotocamel.model.IntegrationMapping;
//import org.springframework.data.jpa.domain.Specification;
//
//import jakarta.persistence.criteria.Predicate;
//import java.time.LocalDateTime;
//
//public class IntegrationMappingSpecification {
//
//    public static Specification<IntegrationMapping> apiNameContains(String apiName) {
//        return (root, query, criteriaBuilder) -> {
//            if (apiName == null || apiName.trim().isEmpty()) {
//                return null;
//            }
//            return criteriaBuilder.like(
//                    criteriaBuilder.lower(root.get("apiName")),
//                    "%" + apiName.toLowerCase().trim() + "%"
//            );
//        };
//    }
//
//    public static Specification<IntegrationMapping> externalSystemContains(String externalSystem) {
//        return (root, query, criteriaBuilder) -> {
//            if (externalSystem == nulIntegratedApiSpecificationl || externalSystem.trim().isEmpty()) {
//                return null;
//            }
//            return criteriaBuilder.like(
//                    criteriaBuilder.lower(root.get("externalSystem")),
//                    "%" + externalSystem.toLowerCase().trim() + "%"
//            );
//        };
//    }
//
//    public static Specification<IntegrationMapping> datasetIdContains(String datasetId) {
//        return (root, query, criteriaBuilder) -> {
//            if (datasetId == null || datasetId.trim().isEmpty()) {
//                return null;
//            }
//            return criteriaBuilder.like(
//                    criteriaBuilder.lower(root.get("datasetId")),
//                    "%" + datasetId.toLowerCase().trim() + "%"
//            );
//        };
//    }
//
//    public static Specification<IntegrationMapping> dataElementIdContains(String dataElementId) {
//        return (root, query, criteriaBuilder) -> {
//            if (dataElementId == null || dataElementId.trim().isEmpty()) {
//                return null;
//            }
//            return criteriaBuilder.like(
//                    criteriaBuilder.lower(root.get("dataElementId")),
//                    "%" + dataElementId.toLowerCase().trim() + "%"
//            );
//        };
//    }
//
//    public static Specification<IntegrationMapping> externalKeyContains(String externalKey) {
//        return (root, query, criteriaBuilder) -> {
//            if (externalKey == null || externalKey.trim().isEmpty()) {
//                return null;
//            }
//            return criteriaBuilder.like(
//                    criteriaBuilder.lower(root.get("externalKey")),
//                    "%" + externalKey.toLowerCase().trim() + "%"
//            );
//        };
//    }
//
//    public static Specification<IntegrationMapping> notesContains(String notes) {
//        return (root, query, criteriaBuilder) -> {
//            if (notes == null || notes.trim().isEmpty()) {
//                return null;
//            }
//            return criteriaBuilder.like(
//                    criteriaBuilder.lower(root.get("notes")),
//                    "%" + notes.toLowerCase().trim() + "%"
//            );
//        };
//    }
//
//    public static Specification<IntegrationMapping> hasActiveStatus(Boolean isActive) {
//        return (root, query, criteriaBuilder) -> {
//            if (isActive == null) {
//                return null;
//            }
//            return criteriaBuilder.equal(root.get("isActive"), isActive);
//        };
//    }
//
//    public static Specification<IntegrationMapping> createdAfter(LocalDateTime createdAfter) {
//        return (root, query, criteriaBuilder) -> {
//            if (createdAfter == null) {
//                return null;
//            }
//            return criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), createdAfter);
//        };
//    }
//
//    public static Specification<IntegrationMapping> createdBefore(LocalDateTime createdBefore) {
//        return (root, query, criteriaBuilder) -> {
//            if (createdBefore == null) {
//                return null;
//            }
//            return criteriaBuilder.lessThanOrEqualTo(root.get("createdAt"), createdBefore);
//        };
//    }
//
//    public static Specification<IntegrationMapping> createdBetween(LocalDateTime startDate, LocalDateTime endDate) {
//        return (root, query, criteriaBuilder) -> {
//            if (startDate == null || endDate == null) {
//                return null;
//            }
//            return criteriaBuilder.between(root.get("createdAt"), startDate, endDate);
//        };
//    }
//
//    public static Specification<IntegrationMapping> categoryOptionComboIdContains(String categoryOptionComboId) {
//        return (root, query, criteriaBuilder) -> {
//            if (categoryOptionComboId == null || categoryOptionComboId.trim().isEmpty()) {
//                return null;
//            }
//            return criteriaBuilder.like(
//                    criteriaBuilder.lower(root.get("categoryOptionComboId")),
//                    "%" + categoryOptionComboId.toLowerCase().trim() + "%"
//            );
//        };
//    }
//
//    public static Specification<IntegrationMapping> attributeOptionComboIdContains(String attributeOptionComboId) {
//        return (root, query, criteriaBuilder) -> {
//            if (attributeOptionComboId == null || attributeOptionComboId.trim().isEmpty()) {
//                return null;
//            }
//            return criteriaBuilder.like(
//                    criteriaBuilder.lower(root.get("attributeOptionComboId")),
//                    "%" + attributeOptionComboId.toLowerCase().trim() + "%"
//            );
//        };
//    }
//}
package com.middleware.backend.kaotocamel.spec;

import com.middleware.backend.kaotocamel.model.IntegratedApi;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Comprehensive specification for filtering IntegratedApi entities
 * Supports filtering on all fields with powerful search capabilities
 */
public class IntegratedApiSpecification {

    /**
     * Filter by code (partial match, case-insensitive)
     */
    public static Specification<IntegratedApi> codeContains(String code) {
        return (root, query, criteriaBuilder) -> {
            if (code == null || code.trim().isEmpty()) {
                return null;
            }
            return criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("code")),
                    "%" + code.toLowerCase().trim() + "%"
            );
        };
    }

    /**
     * Filter by exact code match
     */
    public static Specification<IntegratedApi> codeEquals(String code) {
        return (root, query, criteriaBuilder) -> {
            if (code == null || code.trim().isEmpty()) {
                return null;
            }
            return criteriaBuilder.equal(
                    criteriaBuilder.lower(root.get("code")),
                    code.toLowerCase().trim()
            );
        };
    }

    /**
     * Filter by name (partial match, case-insensitive)
     */
    public static Specification<IntegratedApi> nameContains(String name) {
        return (root, query, criteriaBuilder) -> {
            if (name == null || name.trim().isEmpty()) {
                return null;
            }
            return criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("name")),
                    "%" + name.toLowerCase().trim() + "%"
            );
        };
    }

    /**
     * Filter by API URL (partial match, case-insensitive)
     */
    public static Specification<IntegratedApi> apiUrlContains(String apiUrl) {
        return (root, query, criteriaBuilder) -> {
            if (apiUrl == null || apiUrl.trim().isEmpty()) {
                return null;
            }
            return criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("apiUrl")),
                    "%" + apiUrl.toLowerCase().trim() + "%"
            );
        };
    }

    /**
     * Filter by API type
     */
    public static Specification<IntegratedApi> typeEquals(String type) {
        return (root, query, criteriaBuilder) -> {
            if (type == null || type.trim().isEmpty()) {
                return null;
            }
            try {
                IntegratedApi.ApiType apiType = IntegratedApi.ApiType.valueOf(type.toUpperCase().trim());
                return criteriaBuilder.equal(root.get("type"), apiType);
            } catch (IllegalArgumentException e) {
                return criteriaBuilder.disjunction(); // Return false condition
            }
        };
    }

    /**
     * Filter by multiple API types
     */
    public static Specification<IntegratedApi> typeIn(List<String> types) {
        return (root, query, criteriaBuilder) -> {
            if (types == null || types.isEmpty()) {
                return null;
            }

            List<IntegratedApi.ApiType> apiTypes = new ArrayList<>();
            for (String type : types) {
                try {
                    apiTypes.add(IntegratedApi.ApiType.valueOf(type.toUpperCase().trim()));
                } catch (IllegalArgumentException e) {
                    // Skip invalid types
                }
            }

            if (apiTypes.isEmpty()) {
                return criteriaBuilder.disjunction();
            }

            return root.get("type").in(apiTypes);
        };
    }

    /**
     * Filter by integrated system (partial match, case-insensitive)
     */
    public static Specification<IntegratedApi> integratedSystemContains(String integratedSystem) {
        return (root, query, criteriaBuilder) -> {
            if (integratedSystem == null || integratedSystem.trim().isEmpty()) {
                return null;
            }
            return criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("integratedSystem")),
                    "%" + integratedSystem.toLowerCase().trim() + "%"
            );
        };
    }

    /**
     * Filter by exact integrated system match
     */
    public static Specification<IntegratedApi> integratedSystemEquals(String integratedSystem) {
        return (root, query, criteriaBuilder) -> {
            if (integratedSystem == null || integratedSystem.trim().isEmpty()) {
                return null;
            }
            return criteriaBuilder.equal(
                    criteriaBuilder.lower(root.get("integratedSystem")),
                    integratedSystem.toLowerCase().trim()
            );
        };
    }

    /**
     * Filter by active status
     */
    public static Specification<IntegratedApi> hasActiveStatus(Boolean isActive) {
        return (root, query, criteriaBuilder) -> {
            if (isActive == null) {
                return null;
            }
            return criteriaBuilder.equal(root.get("isActive"), isActive);
        };
    }

    /**
     * Filter by description (partial match, case-insensitive)
     */
    public static Specification<IntegratedApi> descriptionContains(String description) {
        return (root, query, criteriaBuilder) -> {
            if (description == null || description.trim().isEmpty()) {
                return null;
            }
            return criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("description")),
                    "%" + description.toLowerCase().trim() + "%"
            );
        };
    }

    /**
     * Filter by creation date range
     */
    public static Specification<IntegratedApi> createdBetween(LocalDateTime startDate, LocalDateTime endDate) {
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
    public static Specification<IntegratedApi> createdAfter(LocalDateTime createdAfter) {
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
    public static Specification<IntegratedApi> createdBefore(LocalDateTime createdBefore) {
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
    public static Specification<IntegratedApi> updatedBetween(LocalDateTime startDate, LocalDateTime endDate) {
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
    public static Specification<IntegratedApi> globalSearch(String searchTerm) {
        return (root, query, criteriaBuilder) -> {
            if (searchTerm == null || searchTerm.trim().isEmpty()) {
                return null;
            }

            String searchPattern = "%" + searchTerm.toLowerCase().trim() + "%";

            return criteriaBuilder.or(
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("code")), searchPattern),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), searchPattern),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("apiUrl")), searchPattern),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("integratedSystem")), searchPattern),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("description")), searchPattern)
            );
        };
    }

    /**
     * Filter by ID range
     */
    public static Specification<IntegratedApi> idBetween(Long minId, Long maxId) {
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
    public static Specification<IntegratedApi> buildSpecification(
            String code,
            String name,
            String apiUrl,
            String type,
            String integratedSystem,
            Boolean isActive,
            String description,
            LocalDateTime createdAfter,
            LocalDateTime createdBefore,
            LocalDateTime updatedAfter,
            LocalDateTime updatedBefore,
            String globalSearch,
            Long minId,
            Long maxId
    ) {
        return Specification.where(codeContains(code))
                .and(nameContains(name))
                .and(apiUrlContains(apiUrl))
                .and(typeEquals(type))
                .and(integratedSystemContains(integratedSystem))
                .and(hasActiveStatus(isActive))
                .and(descriptionContains(description))
                .and(createdAfter(createdAfter))
                .and(createdBefore(createdBefore))
                .and(updatedBetween(updatedAfter, updatedBefore))
                .and(globalSearch(globalSearch))
                .and(idBetween(minId, maxId));
    }

}