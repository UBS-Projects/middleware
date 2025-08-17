package com.middleware.backend.scheduledJobs.service;

import com.middleware.backend.scheduledJobs.DTO.JobExecutionLogDTO;
import com.middleware.backend.scheduledJobs.DTO.LogResponse;
import com.middleware.backend.scheduledJobs.model.ExecutionHistory;
import com.middleware.backend.scheduledJobs.model.JobExecutionLogs;
import com.middleware.backend.scheduledJobs.repository.JobExecutionLogsRepository;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@AllArgsConstructor
public class JobExecutionlogsService {
    private final JobExecutionLogsRepository repo;


    public ResponseEntity<?> getAll(Specification<JobExecutionLogs> spec, Pageable pageable) {
        Page<JobExecutionLogs> page = repo.findAll(spec, pageable);
        Page<JobExecutionLogDTO> dtoPage = page.map(JobExecutionLogDTO::fromEntity);
        return ResponseEntity.ok(dtoPage);
    }

    public ResponseEntity<?> getById(Long id) {
        Optional<JobExecutionLogs> log = repo.findById(id);
        return log.isEmpty() ? ResponseEntity.notFound().build() : ResponseEntity.ok(log.map(LogResponse::fromEntity));
    }
}
