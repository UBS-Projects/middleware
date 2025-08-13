package com.middleware.backend.scheduledJobs.repository;

import com.middleware.backend.scheduledJobs.model.ExecutionHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LogsRepository extends JpaRepository<ExecutionHistory,Long> {
    Page<ExecutionHistory> findAll(Specification<ExecutionHistory> spec, Pageable pageable);
}
