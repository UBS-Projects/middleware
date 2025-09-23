package com.middleware.backend.repository;

import com.middleware.backend.model.SourceSystem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data repository for {@link com.middleware.backend.model.SourceSystem} entities.
 * <p>
 * Supports uniqueness checks, fetching active records, usage counts in mappings,
 * and specification-based pagination queries.
 */
@Repository
public interface SourceSystemRepository extends JpaRepository<SourceSystem, Long> {

    /**
     * Checks whether a source system with the given name already exists (case-insensitive).
     *
     * @param name source system name to check
     * @return true if a system exists with that name; false otherwise
     */
    boolean existsByNameIgnoreCase(String name);

    /**
     * Retrieves all source systems marked as active.
     *
     * @return list of active systems
     */
    List<SourceSystem> findByActiveTrue();

    /**
     * Counts how many error mappings reference the given source system id.
     *
     * @param sourceSystemId id of the source system
     * @return number of mappings referencing the source system
     */
    @Query("SELECT COUNT(em) FROM ErrorMapping em WHERE em.sourceSystem.id = :sourceSystemId")
    long countErrorMappingsBySourceSystemId(Long sourceSystemId);

    /**
     * Returns a page of source systems that match the provided specification.
     *
     * @param spec     specification to filter by; may be null
     * @param pageable paging and sorting information
     * @return page of matching systems
     */
    Page<SourceSystem> findAll(Specification<SourceSystem> spec, Pageable pageable);
}