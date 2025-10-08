package com.middleware.backend.integrated_systems.repository;

import com.middleware.backend.integrated_systems.model.IntegratedSystem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for accessing {@link IntegratedSystem} entities.
 * <p>
 * Extends JpaRepository to provide basic CRUD operations and supports
 * filtering with JPA Specifications and pagination.
 */
@Repository
public interface IntegratedSystemRepository extends JpaRepository<IntegratedSystem, Long> {

    /**
     * Retrieves a paginated list of integrated systems matching the given specification.
     *
     * @param spec     JPA specification containing filtering conditions
     * @param pageable pagination and sorting information
     * @return a page of {@link IntegratedSystem} entities matching the specification
     */
    Page<IntegratedSystem> findAll(Specification<IntegratedSystem> spec, Pageable pageable);

    /**
     * Retrieves a single integrated system by its unique code.
     *
     * @param code the unique code of the integrated system
     * @return an {@link Optional} containing the system if found, or empty if not found
     */
    Optional<IntegratedSystem> findByCode(String code);
}
