package com.middleware.backend.repository;

import com.middleware.backend.model.ErrorCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ErrorCategoryRepository extends JpaRepository<ErrorCategory, Long> {

    // To check for uniqueness before creating a new category
    boolean existsByNameIgnoreCase(String name);

    // To prevent deleting a category if it's already used
    @Query("SELECT COUNT(em) FROM ErrorMapping em WHERE em.errorCategory.id = :categoryId")
    long countErrorMappingsByCategoryId(Long categoryId);
}