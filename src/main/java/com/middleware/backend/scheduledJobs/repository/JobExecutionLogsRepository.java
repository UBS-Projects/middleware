package com.middleware.backend.scheduledJobs.repository;

import com.middleware.backend.scheduledJobs.model.JobExecutionLogs;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JobExecutionLogsRepository extends JpaRepository<JobExecutionLogs, Long> {
    @EntityGraph(attributePaths = {"job"})
    Page<JobExecutionLogs> findAll(Specification<JobExecutionLogs> spec, Pageable pageable);
}