package com.middleware.backend.kaotocamel.repository;

import com.middleware.backend.kaotocamel.model.IntegratedApi;
import com.middleware.backend.kaotocamel.model.IntegrationMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for IntegratedApi entity
 */
@Repository
public interface IntegratedApiRepository
        extends JpaRepository<IntegratedApi, Long>,
        JpaSpecificationExecutor<IntegratedApi> {
    /**
     * Find API by unique code
     */
    Optional<IntegratedApi> findByCode(String code);

    /**
     * Check if code exists
     */
    boolean existsByCode(String code);

}