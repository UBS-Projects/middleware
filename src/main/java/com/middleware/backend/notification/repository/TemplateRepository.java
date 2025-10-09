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

/**
 * Repository interface for managing {@link NotificationTemplate} entities.
 *
 * <p>Provides standard CRUD operations, as well as custom methods for querying
 * templates by name, code, active status, and for retrieving paginated results
 * using specifications.</p>
 */
@Repository
public interface TemplateRepository extends JpaRepository<NotificationTemplate, Long> {

    /**
     * Retrieves a paginated list of notification templates matching the given specification.
     *
     * @param spec     the filtering specification
     * @param pageable the pagination information
     * @return a page of {@link NotificationTemplate} entities
     */
    Page<NotificationTemplate> findAll(Specification<NotificationTemplate> spec, Pageable pageable);

    /**
     * Finds a notification template by its exact name.
     *
     * @param name the name of the template
     * @return an {@link Optional} containing the matching {@link NotificationTemplate} if found
     */
    Optional<NotificationTemplate> findByName(String name);

    /**
     * Finds up to 5 active notification templates whose names contain the given search string (case-insensitive).
     *
     * @param search the substring to search for in template names
     * @return a list of matching {@link NotificationTemplate} entities
     */
    List<NotificationTemplate> findTop5ByNameContainingIgnoreCaseAndActiveTrue(String search);

    /**
     * Finds a notification template by its exact code.
     *
     * @param code the code of the template
     * @return an {@link Optional} containing the matching {@link NotificationTemplate} if found
     */
    Optional<NotificationTemplate> findByCode(String code);

    /**
     * Retrieves all active notification templates with pagination.
     *
     * @param pageRequest the pagination information
     * @return a list of active {@link NotificationTemplate} entities
     */
    List<NotificationTemplate> findAllByActiveTrue(PageRequest pageRequest);
}
