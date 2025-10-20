package com.middleware.backend.scheduledJobs.repository;

import com.middleware.backend.dashboard.dto.ScheduledExecutedJobsListDto;
import com.middleware.backend.scheduledJobs.enums.Status;
import com.middleware.backend.scheduledJobs.model.JobExecutionLogs;
import com.middleware.backend.scheduledJobs.model.ScheduledJobs;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

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



    long countAllByStartTimeBetween(Timestamp startOfDay, Timestamp endOfDay);
    @Query("""

            SELECT new com.middleware.backend.dashboard.dto.ScheduledExecutedJobsListDto(
     l.id,
     j.jobName,
     l.status || '',
     l.startTime,
     l.durationMs
 )
FROM JobExecutionLogs l
JOIN l.job j
ORDER BY l.startTime DESC
""")
    List<ScheduledExecutedJobsListDto> findTop10JobDtos(Pageable pageable);
    @Query(value = """
    SELECT DATE(start_time) AS date,
           COUNT(*) AS total,
           SUM(CASE WHEN status = 'SUCCESS' THEN 1 ELSE 0 END) AS succeeded,
           SUM(CASE WHEN status = 'FAILURE' THEN 1 ELSE 0 END) AS failed
    FROM job_execution_logs
    WHERE start_time >= NOW() - INTERVAL '30 days'
    GROUP BY DATE(start_time)
    ORDER BY DATE(start_time)
    """, nativeQuery = true)
    List<Map<String, Object>> findJobGraphDataLast30Days();
}