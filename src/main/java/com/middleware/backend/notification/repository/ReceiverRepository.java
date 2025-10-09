package com.middleware.backend.notification.repository;
import com.middleware.backend.notification.dto.ReceiverDto;
import com.middleware.backend.notification.model.Receiver;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for managing {@link Receiver} entities.
 *
 * <p>Provides standard CRUD operations, as well as custom methods for querying
 * receivers by phone, email, name, and paginated/filterable results using specifications.</p>
 */
@Repository
public interface ReceiverRepository extends JpaRepository<Receiver, Long> {

    /**
     * Retrieves a paginated list of receivers matching the given specification.
     *
     * @param spec     the filtering specification
     * @param pageable the pagination information
     * @return a page of {@link Receiver} entities
     */
    Page<Receiver> findAll(Specification<Receiver> spec, Pageable pageable);

    /**
     * Finds a receiver by their exact phone number.
     *
     * @param phone the phone number of the receiver
     * @return an {@link Optional} containing the matching {@link Receiver} if found
     */
    Optional<Receiver> findByPhone(String phone);

    /**
     * Finds a receiver by their exact email address.
     *
     * @param email the email address of the receiver
     * @return an {@link Optional} containing the matching {@link Receiver} if found
     */
    Optional<Receiver> findByEmail(String email);

    /**
     * Finds up to 5 receivers whose names contain the given search string (case-insensitive).
     *
     * @param name the substring to search for in receiver names
     * @return a list of matching {@link Receiver} entities
     */
    List<Receiver> findTop5ByNameContainingIgnoreCase(String name);
}
