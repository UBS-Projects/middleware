package com.middleware.backend.kaotocamel.repository;

import com.middleware.backend.kaotocamel.model.IntegratedApi;
import com.middleware.backend.kaotocamel.model.IntegrationMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for IntegrationMapping entity
 */
@Repository
public interface IntegrationMappingRepository
        extends JpaRepository<IntegrationMapping, Long>,
        JpaSpecificationExecutor<IntegrationMapping> {

    /**
     * Find all mappings for a middleware API
     */
    List<IntegrationMapping> findByMiddlewareApiNameAndIsActiveTrue(String middlewareApiName);

    /**
     * Find all active mappings for a middleware API
     */
    @Query("SELECT m FROM IntegrationMapping m " +
            "JOIN FETCH m.integratedApi api " +
            "WHERE m.middlewareApiName = :apiName " +
            "AND m.isActive = true " +
            "AND api.isActive = true " +
            "ORDER BY m.integratedApiId, m.mappingType, m.data")
    List<IntegrationMapping> findActiveMiddlewareMappings(@Param("apiName") String apiName);


    /**
     * Check if external key exists for a middleware API
     */
    boolean existsByMiddlewareApiNameAndExternalKeyAndIsActiveTrue(
            String middlewareApiName, String externalKey);


    /**
     * Get distinct middleware API names
     */
    @Query("SELECT DISTINCT m.middlewareApiName FROM IntegrationMapping m " +
            "WHERE m.isActive = true")
    List<String> findDistinctMiddlewareApiNames();


}