package com.middleware.backend.camel.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;

import com.middleware.backend.camel.model.DynamicRouteAudit;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

/**
 * Repository for accessing audit records of dynamic route operations.
 * Provides filter-based search and specification support for paging and querying logs.
 */
public interface DynamicRouteAuditRepository extends JpaRepository<DynamicRouteAudit, Long> {
    /**
     * Retrieves audit records matching the provided optional filters. Any {@code null} parameter is ignored.
     *
     * @param id the audit record ID to match, or {@code null} to ignore
     * @param routeId the route identifier to match, or {@code null} to ignore
     * @param version the route version to match, or {@code null} to ignore
     * @param action the action name to match (e.g., upload, start, stop), or {@code null} to ignore
     * @param details a substring to search within the details field, or {@code null} to ignore
     * @param timestamp the exact timestamp to match, or {@code null} to ignore
     * @param pageable paging and sorting information
     * @return a page of {@link DynamicRouteAudit} matching the filters
     */
    @Query("SELECT d FROM DynamicRouteAudit d " +
            "WHERE (:id IS NULL OR d.id = :id) " +
            "AND (:routeId IS NULL OR d.routeId = :routeId) " +
            "AND (:version IS NULL OR d.version = :version) " +
            "AND (:action IS NULL OR d.action = :action) " +
            "AND (:details IS NULL OR d.details LIKE %:details%) " +
            "AND (:timestamp IS NULL OR d.timestamp = :timestamp)")
    Page<DynamicRouteAudit> findByFilters(
            @Param("id") Long id,
            @Param("routeId") String routeId,
            @Param("version") Integer version,
            @Param("action") String action,
            @Param("details") String details,
            @Param("timestamp") LocalDateTime timestamp,
            Pageable pageable);
    /**
     * Finds audit logs using a JPA {@link Specification} with pagination.
     *
     * @param spec the filtering specification
     * @param pageable paging and sorting information
     * @return a page of {@link DynamicRouteAudit} matching the specification
     */
    Page<DynamicRouteAudit> findAll(Specification<DynamicRouteAudit> spec, Pageable pageable);
}
