package com.middleware.backend.repository;


import org.springframework.data.jpa.repository.JpaRepository;

import com.middleware.backend.model.AuditLog;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
}
