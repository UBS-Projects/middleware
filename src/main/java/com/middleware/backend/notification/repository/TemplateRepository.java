package com.middleware.backend.notification.repository;

import aj.org.objectweb.asm.commons.Remapper;
import com.middleware.backend.notification.model.NotificationTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface TemplateRepository extends JpaRepository<NotificationTemplate, Long> {
    Page<NotificationTemplate> findAll(Specification<NotificationTemplate> spec, Pageable pageable);
    Optional<NotificationTemplate> findByName(String name);

    List<NotificationTemplate> findTop5ByNameContainingIgnoreCaseAndActiveTrue(String search);

    Optional<NotificationTemplate> findByCode(String trim);

    List<NotificationTemplate> findAllByActiveTrue(PageRequest of);
}