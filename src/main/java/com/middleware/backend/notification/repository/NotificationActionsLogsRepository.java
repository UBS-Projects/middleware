package com.middleware.backend.notification.repository;

import com.middleware.backend.notification.model.NotificationActionsLogs;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for managing {@link NotificationActionsLogs} entities.
 *
 * <p>Provides standard CRUD operations, as well as a method for retrieving
 * paginated and filtered results using JPA Specifications.</p>
 */
@Repository
public interface NotificationActionsLogsRepository extends JpaRepository<NotificationActionsLogs, Long> {

    /**
     * Retrieves a paginated list of notification action logs matching the given specification.
     *
     * @param spec     the filtering specification
     * @param pageable the pagination information
     * @return a page of {@link NotificationActionsLogs} entities
     */
    Page<NotificationActionsLogs> findAll(Specification<NotificationActionsLogs> spec, Pageable pageable);
}
