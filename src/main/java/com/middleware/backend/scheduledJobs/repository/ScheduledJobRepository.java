package com.middleware.backend.scheduledJobs.repository;

import com.middleware.backend.kaotocamel.model.DynamicRouteEntity;
import com.middleware.backend.scheduledJobs.model.ScheduledJobs;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for CRUD and custom queries on {@link ScheduledJobs} definitions.
 */
@Repository
public interface ScheduledJobRepository extends JpaRepository<ScheduledJobs,Long> {
    /**
     * Finds all job definitions that are currently enabled.
     *
     * @return list of enabled jobs
     */
    List<ScheduledJobs> findByEnabledTrue();

    /**
     * Looks up an active job by its unique name.
     *
     * @param name job name
     * @return optional job if found and active
     */
    Optional<ScheduledJobs> findByJobNameAndActiveTrue(String name);

    /**
     * Finds an active job by fully matching its API endpoint, method, headers, and payload.
     *
     * @param apiEndPoint target API endpoint URL or path
     * @param headers serialized request headers
     * @param payload serialized request body
     * @param method HTTP method
     * @return matching active job or null when not found
     */
    ScheduledJobs findByApiEndpointAndMethodAndHeadersAndPayloadAndActiveTrue(String apiEndPoint,String headers,String payload, String method);

    /**
     * Finds a page of jobs matching the provided specification.
     *
     * @param spec dynamic criteria
     * @param pageable pagination info
     * @return page of matching jobs
     */
    Page<ScheduledJobs> findAll(Specification<ScheduledJobs> spec, Pageable pageable);
}
