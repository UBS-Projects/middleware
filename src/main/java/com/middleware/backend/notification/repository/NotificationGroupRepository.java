package com.middleware.backend.notification.repository;

import com.middleware.backend.notification.dto.NotificationGroupDto;
import com.middleware.backend.notification.dto.ReceiverRequest;
import com.middleware.backend.notification.model.NotificationGroup;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for managing {@link NotificationGroup} entities.
 *
 * <p>Provides standard CRUD operations, as well as custom methods for querying
 * groups by name, code, and active status, and for retrieving groups based
 * on whether they have associated receivers.</p>
 */
@Repository
public interface NotificationGroupRepository extends JpaRepository<NotificationGroup, Long> {

    /**
     * Retrieves a paginated list of notification groups matching the given specification.
     *
     * @param spec     the filtering specification
     * @param pageable the pagination information
     * @return a page of {@link NotificationGroup} entities
     */
    Page<NotificationGroup> findAll(Specification<NotificationGroup> spec, Pageable pageable);

    /**
     * Finds a notification group by its exact name.
     *
     * @param name the name of the group
     * @return an {@link Optional} containing the matching {@link NotificationGroup} if found
     */
    Optional<NotificationGroup> findByName(String name);

    /**
     * Retrieves paginated notification groups that have at least one associated receiver.
     *
     * @param pageable the pagination information
     * @return a page of {@link NotificationGroup} entities with receivers
     */
    @Query("SELECT g FROM NotificationGroup g WHERE g.receivers IS NOT EMPTY")
    Page<NotificationGroup> findAllWithReceivers(Pageable pageable);

    /**
     * Retrieves notification groups that have no associated receivers.
     *
     * @return a list of {@link NotificationGroup} entities without receivers
     */
    @Query("SELECT g FROM NotificationGroup g WHERE g.receivers IS EMPTY")
    List<NotificationGroup> findAllWithoutReceivers();

    /**
     * Finds up to 5 active notification groups whose names contain the given search string (case-insensitive).
     *
     * @param search the substring to search for in group names
     * @return a list of matching {@link NotificationGroup} entities
     */
    List<NotificationGroup> findTop5ByNameContainingIgnoreCaseAndActiveTrue(String search);

    /**
     * Finds a notification group by its exact code.
     *
     * @param code the code of the group
     * @return an {@link Optional} containing the matching {@link NotificationGroup} if found
     */
    Optional<NotificationGroup> findByCode(String code);

    /**
     * Retrieves all active notification groups with pagination.
     *
     * @param pageRequest the pagination information
     * @return a list of active {@link NotificationGroup} entities
     */
    List<NotificationGroup> findAllByActiveTrue(PageRequest pageRequest);

    Optional<NotificationGroup> findByCodeAndActiveTrue(String groupId);
}
