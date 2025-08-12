package com.middleware.backend.kaotocamel.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;

import com.middleware.backend.kaotocamel.model.DynamicRouteAudit;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface DynamicRouteAuditRepository extends JpaRepository<DynamicRouteAudit, Long> {
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

    Page<DynamicRouteAudit> findAll(Specification<DynamicRouteAudit> spec, Pageable pageable);
}
