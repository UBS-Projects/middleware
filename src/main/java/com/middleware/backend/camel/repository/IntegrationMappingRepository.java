package com.middleware.backend.camel.repository;

import com.middleware.backend.camel.model.IntegrationMapping;
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

    /**
     * Find active mappings for a given integratedApiId.
     * Used to deactivate mappings when the parent IntegratedApi is deactivated.
     */
    List<IntegrationMapping> findByIntegratedApiIdAndIsActiveTrue(Long integratedApiId);

    /**
     * Count active mappings for a given integratedApiId.
     * Used to check if an IntegratedApi can be deactivated.
     */
    long countByIntegratedApiIdAndIsActiveTrue(Long integratedApiId);
    boolean existsByDynamicRouteIdAndIntegratedApiIdAndMappingTypeAndDataAndExternalKey(
            String dynamicRouteId,
            Long integratedApiId,
            IntegrationMapping.MappingType mappingType,
            String data,
            String externalKey
    );
    @Query("SELECT CASE WHEN COUNT(m) > 0 THEN true ELSE false END FROM IntegrationMapping m " +
            "WHERE m.dynamicRouteId = :dynamicRouteId " +
            "AND m.integratedApi.id = :integratedApiId " +
            "AND m.mappingType = :mappingType " +
            "AND m.data = :data " +
            "AND m.externalKey = :externalKey " +
            "AND m.id != :excludeId")
    boolean existsByUniqueConstraintExcludingId(
            @Param("dynamicRouteId") String dynamicRouteId,
            @Param("integratedApiId") Long integratedApiId,
            @Param("mappingType") IntegrationMapping.MappingType mappingType,
            @Param("data") String data,
            @Param("externalKey") String externalKey,
            @Param("excludeId") Long excludeId
    );
}