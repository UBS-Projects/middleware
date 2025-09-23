package com.middleware.backend.scheduledJobs.repository;

import com.middleware.backend.scheduledJobs.model.JobExecutionLogs;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for querying {@link JobExecutionLogs} with support for dynamic filtering and eager loading of job.
 */
@Repository
public interface JobExecutionLogsRepository extends JpaRepository<JobExecutionLogs, Long> {
    /**
     * Finds a page of execution logs matching the given specification.
     * Eagerly fetches the associated job to avoid N+1 lookups.
     *
     * @param spec JPA Specification for dynamic filters
     * @param pageable pagination information
     * @return page of execution logs
     */
    @EntityGraph(attributePaths = {"job"})
    Page<JobExecutionLogs> findAll(Specification<JobExecutionLogs> spec, Pageable pageable);
}