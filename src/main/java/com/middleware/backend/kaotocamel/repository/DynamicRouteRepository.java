// 1. Repository - Native Query محسّن مع أسماء الأعمدة الصحيحة
package com.middleware.backend.kaotocamel.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.middleware.backend.kaotocamel.model.DynamicRouteEntity;

public interface DynamicRouteRepository
        extends JpaRepository<DynamicRouteEntity, Long>, JpaSpecificationExecutor<DynamicRouteEntity> {

        List<DynamicRouteEntity> findByRouteIdOrderByVersionDesc(String routeId);

        Optional<DynamicRouteEntity> findByRouteIdAndVersion(String routeId, int version);

        List<DynamicRouteEntity> findByRouteIdAndActiveTrue(String routeId);

        Page<DynamicRouteEntity> findByActiveTrue(Pageable pageable);

        Page<DynamicRouteEntity> findByRouteId(String routeId, Pageable pageable);

        /**
         * Native Query optimized for performance
         * Uses explicit column mapping to avoid JPA column name conflicts
         */
        @Query(value = """
        SELECT 
            r.id as id,
            r.route_id as routeId,
            r.description as description,
            r.version as version,
            r.path as path,
            r.http_method as httpMethod,
            r.yaml_content as yamlContent,
            r.active as active,
            r.default_version as defaultVersion,
            r.created_at as createdAt,
            r.comment as comment
        FROM (
            SELECT *,
                   ROW_NUMBER() OVER (
                       PARTITION BY route_id 
                       ORDER BY 
                           active DESC,
                           default_version DESC,
                           version DESC,
                           created_at DESC
                   ) as rn
            FROM dynamic_routes
        ) r 
        WHERE r.rn = 1
        ORDER BY r.created_at DESC
        LIMIT :limit OFFSET :offset
        """,
                nativeQuery = true)
        List<Object[]> findLatestRoutesNative(@Param("limit") int limit, @Param("offset") int offset);

        @Query(value = """
        SELECT COUNT(DISTINCT route_id) 
        FROM dynamic_routes
        """,
                nativeQuery = true)
        long countDistinctRouteIds();

        /**
         * Native Query with filters
         */
        @Query(value = """
        SELECT 
            r.id, r.route_id, r.description, r.version, r.path, r.http_method,
            r.yaml_content, r.active, r.default_version, r.created_at, r.comment
        FROM (
            SELECT *,
                   ROW_NUMBER() OVER (
                       PARTITION BY route_id 
                       ORDER BY 
                           active DESC,
                           default_version DESC,
                           version DESC,
                           created_at DESC
                   ) as rn
            FROM dynamic_routes
            WHERE (:routeId IS NULL OR route_id ILIKE CONCAT('%', :routeId, '%'))
              AND (:active IS NULL OR active = :active)
              AND (:yamlContains IS NULL OR yaml_content ILIKE CONCAT('%', :yamlContains, '%'))
              AND (:httpMethod IS NULL OR http_method ILIKE CONCAT('%', :httpMethod, '%'))
              AND (:path IS NULL OR path ILIKE '%' || :path || '%')
              AND (:description IS NULL OR description ILIKE CONCAT('%', :description, '%'))
              AND (:comment IS NULL OR comment ILIKE CONCAT('%', :comment, '%'))
              AND (:createdAfter IS NULL OR created_at >= :createdAfter)
              AND (:createdBefore IS NULL OR created_at <= :createdBefore)
        ) r 
        WHERE r.rn = 1
        ORDER BY r.created_at DESC
        LIMIT :limit OFFSET :offset
        """,
                nativeQuery = true)
        List<Object[]> findLatestRoutesWithFiltersNative(
                @Param("routeId") String routeId,
                @Param("active") Boolean active,
                @Param("yamlContains") String yamlContains,
                @Param("httpMethod") String httpMethod,
                @Param("path") String path,
                @Param("description") String description,
                @Param("comment") String comment,
                @Param("createdAfter") LocalDateTime createdAfter,
                @Param("createdBefore") LocalDateTime createdBefore,
                @Param("limit") int limit,
                @Param("offset") int offset);

        @Query(value = """
        SELECT COUNT(DISTINCT route_id) 
        FROM dynamic_routes
        WHERE (:routeId IS NULL OR route_id ILIKE CONCAT('%', :routeId, '%'))
          AND (:active IS NULL OR active = :active)
          AND (:yamlContains IS NULL OR yaml_content ILIKE CONCAT('%', :yamlContains, '%'))
          AND (:httpMethod IS NULL OR http_method ILIKE CONCAT('%', :httpMethod, '%'))
          AND (:path IS NULL OR path ILIKE CONCAT('%', :path, '%'))
          AND (:description IS NULL OR description ILIKE CONCAT('%', :description, '%'))
          AND (:comment IS NULL OR comment ILIKE CONCAT('%', :comment, '%'))
          AND (:createdAfter IS NULL OR created_at >= :createdAfter)
          AND (:createdBefore IS NULL OR created_at <= :createdBefore)
        """,
                nativeQuery = true)
        long countLatestRoutesWithFilters(
                @Param("routeId") String routeId,
                @Param("active") Boolean active,
                @Param("yamlContains") String yamlContains,
                @Param("httpMethod") String httpMethod,
                @Param("path") String path,
                @Param("description") String description,
                @Param("comment") String comment,
                @Param("createdAfter") LocalDateTime createdAfter,
                @Param("createdBefore") LocalDateTime createdBefore);
}