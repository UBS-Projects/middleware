package com.middleware.backend.camel.repository;

import com.middleware.backend.camel.model.IntegratedApi;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for IntegratedApi entity
 */
@Repository
public interface IntegratedApiRepository
        extends JpaRepository<IntegratedApi, Long>,
        JpaSpecificationExecutor<IntegratedApi> {

    /**
     * Find API by unique code
     */
    Optional<IntegratedApi> findByCode(String code);

    /**
     * Check if code exists
     */
    boolean existsByCode(String code);

    /**
     * Find active METADATA API by bound dynamic route ID
     * Used to automatically fetch metadata for dynamic routes
     *
     * @param boundRouteId The dynamic route ID (e.g., "integration-mapping-test")
     * @return Optional containing the metadata API configuration if found
     */
    @Query("SELECT api FROM IntegratedApi api " +
            "WHERE api.type = 'METADATA' " +
            "AND api.boundApiCode = :boundRouteId " +
            "AND api.isActive = true")
    Optional<IntegratedApi> findActiveMetadataByBoundCode(@Param("boundRouteId") String boundRouteId);
    /**
     * Find all active METADATA APIs with their bound codes
     * Useful for listing all available metadata configurations
     *
     * @return List of active metadata APIs
     */
    @Query("SELECT api FROM IntegratedApi api " +
            "WHERE api.type = 'METADATA' " +
            "AND api.isActive = true " +
            "ORDER BY api.boundApiCode")
    List<IntegratedApi> findAllActiveMetadataApis();

    /**
     * Check if a bound API code exists for active metadata APIs
     *
     * @param boundApiCode The business API code to check
     * @return true if exists, false otherwise
     */
    @Query("SELECT COUNT(api) > 0 FROM IntegratedApi api " +
            "WHERE api.type = 'METADATA' " +
            "AND api.boundApiCode = :boundApiCode " +
            "AND api.isActive = true")
    boolean existsActiveMetadataByBoundCode(@Param("boundApiCode") String boundApiCode);
}