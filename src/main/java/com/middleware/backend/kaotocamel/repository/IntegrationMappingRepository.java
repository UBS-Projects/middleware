package com.middleware.backend.kaotocamel.repository;

import com.middleware.backend.kaotocamel.model.IntegrationMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IntegrationMappingRepository
        extends JpaRepository<IntegrationMapping, Long>,
        JpaSpecificationExecutor<IntegrationMapping> {

    /**
     * Find all mappings for a dynamic route
     */
    List<IntegrationMapping> findByDynamicRouteIdAndIsActiveTrue(String dynamicRouteId);

    /**
     * Find mapping by dynamic route ID and external key
     */
    Optional<IntegrationMapping> findByDynamicRouteIdAndExternalKey(
            String dynamicRouteId, String externalKey);

    /**
     * Find all active mappings for a dynamic route with integrated API
     */
    @Query("SELECT m FROM IntegrationMapping m " +
            "JOIN FETCH m.integratedApi api " +
            "WHERE m.dynamicRouteId = :routeId " +  // ← صح
            "AND m.isActive = true " +
            "AND api.isActive = true " +
            "ORDER BY m.integratedApiId, m.mappingType, m.data")
    List<IntegrationMapping> findActiveRouteMappings(@Param("routeId") String routeId);

    /**
     * Check if external key exists for a dynamic route
     */
    boolean existsByDynamicRouteIdAndExternalKeyAndIsActiveTrue(
            String dynamicRouteId, String externalKey);

    /**
     * Get distinct dynamic route IDs
     */
    @Query("SELECT DISTINCT m.dynamicRouteId FROM IntegrationMapping m " +
            "WHERE m.isActive = true")
    List<String> findDistinctDynamicRouteIds();

    /**
     * Count mappings for a dynamic route
     */
    long countByDynamicRouteIdAndIsActiveTrue(String dynamicRouteId);
}