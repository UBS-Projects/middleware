package com.middleware.backend.audit_logs_interceptor.repository;



import com.middleware.backend.audit_logs_interceptor.model.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

/**
 * Spring Data JPA repository for {@link AuditLog} entities.
 * This interface provides the standard CRUD operations for {@link AuditLog} objects
 * and also supports pagination and specification-based queries.
 */
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    /**
     * Finds all {@link AuditLog} entities that match the given specification and page request.
     *
     * @param spec     A {@link Specification} to filter the results. Can be null.
     * @param pageable A {@link Pageable} object for pagination and sorting.
     * @return A {@link Page} of {@link AuditLog} entities.
     */
    Page<AuditLog> findAll(Specification<AuditLog> spec, Pageable pageable);
}
