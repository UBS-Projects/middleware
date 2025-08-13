package com.middleware.backend.scheduledJobs.service;

import com.middleware.backend.scheduledJobs.DTO.JobExecutionDTO;
import com.middleware.backend.scheduledJobs.mapper.LogsMapper;
import com.middleware.backend.scheduledJobs.model.ExecutionHistory;
import com.middleware.backend.scheduledJobs.repository.LogsRepository;
import com.middleware.backend.users.dto.UserResponse;
import com.middleware.backend.users.model.User;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class JobLogsService {
    private final LogsRepository repo;
    public void save(JobExecutionDTO job){
        repo.save(LogsMapper.MapToEntity(job));
    }

    public ResponseEntity<?> getAll(Specification<ExecutionHistory> spec, Pageable pageable) {
        Page<ExecutionHistory> page = repo.findAll(spec, pageable);
        if (page.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(page);
    }
}
