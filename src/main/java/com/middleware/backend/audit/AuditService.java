package com.middleware.backend.audit;

import org.springframework.stereotype.Service;
import com.middleware.backend.model.AuditLog;
import com.middleware.backend.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public void logStep(Long workflowId, Long stepId, String status) {
        AuditLog audit = new AuditLog();
        audit.setWorkflowId(workflowId);
        audit.setStepId(stepId);
        audit.setStatus(status);
        auditLogRepository.save(audit);
    }
}
