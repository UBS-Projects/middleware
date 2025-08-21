package com.middleware.backend.audit_logs_interceptor.repository;



import com.middleware.backend.audit_logs_interceptor.model.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    Page<AuditLog> findAll(Specification<AuditLog> spec, Pageable pageable);
}
