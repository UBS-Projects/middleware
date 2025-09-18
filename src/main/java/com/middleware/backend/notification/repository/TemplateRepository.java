package com.middleware.backend.notification.repository;

import com.middleware.backend.notification.model.NotificationTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TemplateRepository extends JpaRepository<NotificationTemplate, Long> {
    Page<NotificationTemplate> findAll(Specification<NotificationTemplate> spec, Pageable pageable);
    Optional<NotificationTemplate> findByName(String name);
}