package com.middleware.backend.camel.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.middleware.backend.camel.model.DynamicRouteEntity;

/**
 * Repository for managing versioned dynamic routes stored in the database.
 * Supports rich querying for latest versions, active/default flags, and lookups by path/method.
 */
public interface DynamicRouteRepository
        extends JpaRepository<DynamicRouteEntity, Long>, JpaSpecificationExecutor<DynamicRouteEntity> {
        /**
         * Check if a route ID exists (for validation)
         */
        boolean existsByRouteId(String routeId);
        /**
         * Lists all versions for a given route identifier ordered by version descending.
         * @param routeId the logical route identifier
         * @return list of versions newest first
         */
        List<DynamicRouteEntity> findByRouteIdOrderByVersionDesc(String routeId);

        /**
         * Finds a specific version of a route.
         * @param routeId the logical route identifier
         * @param version the version number
         * @return the matching route version if present
         */
        Optional<DynamicRouteEntity> findByRouteIdAndVersion(String routeId, int version);

//        List<DynamicRouteEntity> findByRouteIdAndActiveTrue(String routeId);

        /**
         * Finds the route marked as default for a route identifier.
         * @param routeId the logical route identifier
         * @return the default version entity or {@code null} if none
         */
        DynamicRouteEntity findByRouteIdAndDefaultVersionTrue(String routeId);

        /**
         * Returns active routes as a page.
         */
        Page<DynamicRouteEntity> findByActiveTrue(Pageable pageable);
        /**
         * Returns all active routes.
         */
        List<DynamicRouteEntity> findByActiveTrue();

        /**
         * Returns all route versions for a route identifier as a page.
         */
        Page<DynamicRouteEntity> findByRouteId(String routeId, Pageable pageable);

        /**
         * Counts distinct logical route identifiers.
         */
        @Query("SELECT COUNT(DISTINCT r.routeId) FROM DynamicRouteEntity r")
        long countDistinctRouteIds();

        /**
         * Returns all route versions ordered by creation date descending.
         */
        @Query("SELECT r FROM DynamicRouteEntity r ORDER BY r.createdAt DESC")
        List<DynamicRouteEntity> findAllOrderByCreatedAtDesc();
        // أضف هذه الـ Methods لـ DynamicRouteRepository الموجود عندك

        /**
         * Returns latest active, default versions for all routes.
         */
        @Query("SELECT dr FROM DynamicRouteEntity dr WHERE dr.active = true AND dr.defaultVersion = true ORDER BY dr.routeId")
        List<DynamicRouteEntity> findLatestActiveRoutes();

//        @Query("SELECT dr FROM DynamicRouteEntity dr WHERE dr.routeId = :routeId AND dr.active = true")
//        List<DynamicRouteEntity> findByRouteIdAndActiveTrue(@Param("routeId") String routeId);

        /**
         * Finds the currently active version for a route identifier.
         */
        Optional<DynamicRouteEntity> findByRouteIdAndActiveTrue(String routeId);


        /**
         * Checks if there is an active version for a given routeId.
         */
        @Query("SELECT COUNT(dr) > 0 FROM DynamicRouteEntity dr WHERE dr.routeId = :routeId AND dr.active = true")
        boolean existsByRouteIdAndActiveTrue(@Param("routeId") String routeId);


        /**
         * Finds a route by its REST path regardless of method.
         */
        Optional<DynamicRouteEntity> findByPath(String path);

        // Method to check for duplicate route by path and httpMethod
        /**
         * Finds a route by REST path and HTTP method.
         */
        Optional<DynamicRouteEntity> findByPathAndHttpMethod(String path, String httpMethod);
        /**
         * Finds a route by REST path, HTTP method, and active flag.
         */
        Optional<DynamicRouteEntity> findByPathAndHttpMethodIgnoreCaseAndActive(String path, String method,boolean active);

        /**
         * Finds the first active version for a route identifier.
         */
        Optional<DynamicRouteEntity> findFirstByRouteIdAndActiveTrue(String routeId);

        // Get all distinct routeIds
        /**
         * Lists all distinct logical route identifiers.
         */
        @Query("SELECT DISTINCT r.routeId FROM DynamicRouteEntity r")
        List<String> findAllDistinctRouteIds();

}