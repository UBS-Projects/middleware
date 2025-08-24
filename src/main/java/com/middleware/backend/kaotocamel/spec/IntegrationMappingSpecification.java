package com.middleware.backend.kaotocamel.spec;

import com.middleware.backend.kaotocamel.model.IntegrationMapping;
import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.Predicate;
import java.time.LocalDateTime;

public class IntegrationMappingSpecification {

    public static Specification<IntegrationMapping> apiNameContains(String apiName) {
        return (root, query, criteriaBuilder) -> {
            if (apiName == null || apiName.trim().isEmpty()) {
                return null;
            }
            return criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("apiName")),
                    "%" + apiName.toLowerCase().trim() + "%"
            );
        };
    }

    public static Specification<IntegrationMapping> externalSystemContains(String externalSystem) {
        return (root, query, criteriaBuilder) -> {
            if (externalSystem == null || externalSystem.trim().isEmpty()) {
                return null;
            }
            return criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("externalSystem")),
                    "%" + externalSystem.toLowerCase().trim() + "%"
            );
        };
    }

    public static Specification<IntegrationMapping> datasetIdContains(String datasetId) {
        return (root, query, criteriaBuilder) -> {
            if (datasetId == null || datasetId.trim().isEmpty()) {
                return null;
            }
            return criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("datasetId")),
                    "%" + datasetId.toLowerCase().trim() + "%"
            );
        };
    }

    public static Specification<IntegrationMapping> dataElementIdContains(String dataElementId) {
        return (root, query, criteriaBuilder) -> {
            if (dataElementId == null || dataElementId.trim().isEmpty()) {
                return null;
            }
            return criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("dataElementId")),
                    "%" + dataElementId.toLowerCase().trim() + "%"
            );
        };
    }

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

    public static Specification<IntegrationMapping> hasActiveStatus(Boolean isActive) {
        return (root, query, criteriaBuilder) -> {
            if (isActive == null) {
                return null;
            }
            return criteriaBuilder.equal(root.get("isActive"), isActive);
        };
    }

    public static Specification<IntegrationMapping> createdAfter(LocalDateTime createdAfter) {
        return (root, query, criteriaBuilder) -> {
            if (createdAfter == null) {
                return null;
            }
            return criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), createdAfter);
        };
    }

    public static Specification<IntegrationMapping> createdBefore(LocalDateTime createdBefore) {
        return (root, query, criteriaBuilder) -> {
            if (createdBefore == null) {
                return null;
            }
            return criteriaBuilder.lessThanOrEqualTo(root.get("createdAt"), createdBefore);
        };
    }

    public static Specification<IntegrationMapping> createdBetween(LocalDateTime startDate, LocalDateTime endDate) {
        return (root, query, criteriaBuilder) -> {
            if (startDate == null || endDate == null) {
                return null;
            }
            return criteriaBuilder.between(root.get("createdAt"), startDate, endDate);
        };
    }

    public static Specification<IntegrationMapping> categoryOptionComboIdContains(String categoryOptionComboId) {
        return (root, query, criteriaBuilder) -> {
            if (categoryOptionComboId == null || categoryOptionComboId.trim().isEmpty()) {
                return null;
            }
            return criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("categoryOptionComboId")),
                    "%" + categoryOptionComboId.toLowerCase().trim() + "%"
            );
        };
    }

    public static Specification<IntegrationMapping> attributeOptionComboIdContains(String attributeOptionComboId) {
        return (root, query, criteriaBuilder) -> {
            if (attributeOptionComboId == null || attributeOptionComboId.trim().isEmpty()) {
                return null;
            }
            return criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("attributeOptionComboId")),
                    "%" + attributeOptionComboId.toLowerCase().trim() + "%"
            );
        };
    }
}