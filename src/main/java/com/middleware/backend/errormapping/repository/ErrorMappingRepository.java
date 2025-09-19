package com.middleware.backend.errormapping.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.middleware.backend.errormapping.model.ErrorMapping;

@Repository
public interface ErrorMappingRepository
        extends JpaRepository<ErrorMapping, Long>, JpaSpecificationExecutor<ErrorMapping> {

    @Override
    @EntityGraph(value = "ErrorMapping.withCategoryAndSourceSystem")
    Page<ErrorMapping> findAll(Specification<ErrorMapping> spec, Pageable pageable);

    @EntityGraph(value = "ErrorMapping.withCategoryAndSourceSystem")
    Optional<ErrorMapping> findWithCategoryAndSourceSystemById(Long id);

    @Query("SELECT COUNT(em) FROM ErrorMapping em WHERE em.routeId = :routeId AND em.active = true")
    long countActiveByRouteId(@Param("routeId") String routeId);

    boolean existsByRouteIdAndRawErrorSubstringAndActiveTrue(String routeId, String rawErrorSubstring);

    @Query(value = """
                SELECT em.* FROM error_mapping em
                WHERE em.active = true
                AND (em.route_id = :routeId OR em.route_id = '*')
                AND (em.source_system_id = :sourceSystemId OR em.source_system_id IS NULL)
                AND (
                    (em.match_type = 'CONTAINS' AND LOWER(:errorMessage) LIKE LOWER('%' || em.raw_error_substring || '%')) OR
                    (em.match_type = 'EQUALS' AND :errorMessage = em.raw_error_substring) OR
                    (em.match_type = 'REGEX' AND :errorMessage ~ em.raw_error_substring)
                )
                ORDER BY
                    COALESCE(em.source_system_id, -10) DESC,
                    em.created_at DESC
                LIMIT 1
            """, nativeQuery = true)
    Optional<ErrorMapping> findMatchingErrorWithFallback(@Param("routeId") String routeId,
            @Param("sourceSystemId") Long sourceSystemId, @Param("errorMessage") String errorMessage);

    @EntityGraph(value = "ErrorMapping.withCategoryAndSourceSystem")
    @Query("SELECT em FROM ErrorMapping em WHERE em.id = :id")
    Optional<ErrorMapping> findByIdWithRelations(@Param("id") Long id);
}