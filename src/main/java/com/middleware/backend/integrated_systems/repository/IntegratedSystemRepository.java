package com.middleware.backend.integrated_systems.repository;

import com.middleware.backend.integrated_systems.model.IntegratedSystem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IntegratedSystemRepository extends JpaRepository<IntegratedSystem,Long> {
    Page<IntegratedSystem> findAll(Specification<IntegratedSystem> spec, Pageable pageable);

    Optional<IntegratedSystem> findByCode(String code);
}
