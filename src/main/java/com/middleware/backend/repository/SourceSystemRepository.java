package com.middleware.backend.repository;

import com.middleware.backend.model.SourceSystem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SourceSystemRepository extends JpaRepository<SourceSystem, Long> {

    // To check for uniqueness before creating a new source system
    boolean existsByNameIgnoreCase(String name);

    // To get only active source systems
    List<SourceSystem> findByActiveTrue();

    // To prevent deleting a source system if it's already used
    @Query("SELECT COUNT(em) FROM ErrorMapping em WHERE em.sourceSystem.id = :sourceSystemId")
    long countErrorMappingsBySourceSystemId(Long sourceSystemId);
}