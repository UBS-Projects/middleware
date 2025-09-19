package com.middleware.backend.errormapping.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.middleware.backend.errormapping.model.ErrorCategory;

/**
 * Spring Data repository for {@link com.middleware.backend.model.ErrorCategory} entities.
 * <p>
 * Provides convenience methods for uniqueness checks, fetching active records,
 * usage counts in mappings, and specification-based pagination.
 */
@Repository
public interface ErrorCategoryRepository extends JpaRepository<ErrorCategory, Long> {

    /**
     * Checks whether a category with the given name already exists (case-insensitive).
     *
     * @param name category name to check
     * @return true if a category exists with that name; false otherwise
     */
    boolean existsByNameIgnoreCase(String name);

    /**
     * Retrieves all categories marked as active.
     *
     * @return list of active categories
     */
    List<ErrorCategory> findByActiveTrue();

    /**
     * Counts how many error mappings reference the given category id.
     *
     * @param categoryId id of the category
     * @return number of mappings referencing the category
     */
    @Query("SELECT COUNT(em) FROM ErrorMapping em WHERE em.errorCategory.id = :categoryId")
    long countErrorMappingsByCategoryId(Long categoryId);

    /**
     * Returns a page of categories that match the provided specification.
     *
     * @param spec     specification to filter by; may be null
     * @param pageable paging and sorting information
     * @return page of matching categories
     */
    Page<ErrorCategory> findAll(Specification<ErrorCategory> spec, Pageable pageable);
}