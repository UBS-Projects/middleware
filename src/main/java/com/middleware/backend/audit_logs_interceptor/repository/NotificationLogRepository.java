package com.middleware.backend.audit_logs_interceptor.repository;

import aj.org.objectweb.asm.commons.Remapper;
import com.middleware.backend.notification.model.NotificationLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationLogRepository extends JpaRepository<NotificationLog,Long> {
    Page<NotificationLog> findAll(Specification<NotificationLog> spec, Pageable pageable);
}
