package com.middleware.backend.notification.repository;
import com.middleware.backend.notification.model.ChannelConfig;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for managing {@link ChannelConfig} entities.
 *
 * <p>Provides standard CRUD operations, as well as custom methods for
 * querying by name, code, and active status, and for retrieving
 * paginated and filtered results using JPA Specifications.</p>
 */
@Repository
public interface ChannelConfigRepository extends JpaRepository<ChannelConfig, Long> {

    /**
     * Retrieves a paginated list of channel configurations matching the given specification.
     *
     * @param spec     the filtering specification
     * @param pageable the pagination information
     * @return a page of {@link ChannelConfig} entities
     */
    Page<ChannelConfig> findAll(Specification<ChannelConfig> spec, Pageable pageable);

    /**
     * Finds a channel configuration by its exact name.
     *
     * @param name the name of the channel configuration
     * @return an {@link Optional} containing the matching {@link ChannelConfig} if found
     */
    Optional<ChannelConfig> findByName(String name);

    /**
     * Removes a channel configuration by its ID.
     *
     * @param id the ID of the channel configuration to remove
     */
    void removeById(Long id);

    /**
     * Finds up to 5 active channel configurations whose names contain the given search string (case-insensitive).
     *
     * @param search the substring to search for in channel names
     * @return a list of matching {@link ChannelConfig} entities
     */
    List<ChannelConfig> findTop5ByNameContainingIgnoreCaseAndActiveTrue(String search);

    /**
     * Finds a channel configuration by its exact code.
     *
     * @param code the code of the channel configuration
     * @return an {@link Optional} containing the matching {@link ChannelConfig} if found
     */
    Optional<ChannelConfig> findByCode(String code);

    /**
     * Retrieves all active channel configurations with pagination.
     *
     * @param pageRequest the pagination information
     * @return a list of active {@link ChannelConfig} entities
     */
    List<ChannelConfig> findAllByActiveTrue(PageRequest pageRequest);
}
