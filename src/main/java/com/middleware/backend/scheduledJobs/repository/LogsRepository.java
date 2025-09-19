package com.middleware.backend.scheduledJobs.repository;

import com.middleware.backend.scheduledJobs.model.ExecutionHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for querying audit records of user operations on scheduled jobs.
 */
@Repository
public interface LogsRepository extends JpaRepository<ExecutionHistory,Long> {
    /**
     * Returns a page of audit records matching the provided specification.
     *
     * @param spec dynamic JPA criteria
     * @param pageable pagination information
     * @return page of matching audit records
     */
    Page<ExecutionHistory> findAll(Specification<ExecutionHistory> spec, Pageable pageable);
}
