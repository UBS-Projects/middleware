package com.middleware.backend.kaotocamel.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.middleware.backend.kaotocamel.model.DynamicRouteAudit;

public interface DynamicRouteAuditRepository extends JpaRepository<DynamicRouteAudit, Long> {
}
