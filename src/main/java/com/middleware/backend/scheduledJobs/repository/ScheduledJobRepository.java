package com.middleware.backend.scheduledJobs.repository;

import com.middleware.backend.kaotocamel.model.DynamicRouteEntity;
import com.middleware.backend.scheduledJobs.model.ScheduledJobs;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ScheduledJobRepository extends JpaRepository<ScheduledJobs,Long> {
    List<ScheduledJobs> findByEnabledTrue();
    ScheduledJobs findByApiEndpointAndMethod(String apiEndPoint, String method);
    Page<ScheduledJobs> findAll(Specification<ScheduledJobs> spec, Pageable pageable);
}
