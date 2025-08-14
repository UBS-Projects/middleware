//package com.middleware.backend.service;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.middleware.backend.model.AuditLog;
//import com.middleware.backend.model.WorkflowStep;
//import com.middleware.backend.repository.AuditLogRepository;
//
//import lombok.RequiredArgsConstructor;
//import org.springframework.stereotype.Service;
//
//import java.time.LocalDateTime;
//import java.util.Map;
//
//@Service
//@RequiredArgsConstructor
//public class AuditLogService {
//
//    private final AuditLogRepository auditLogRepository;
//    private final ObjectMapper objectMapper;
//
//    public void logSuccess(WorkflowStep step, Map<String, Object> context, String responseBody, long durationMs) {
//        saveLog(step, context, "SUCCESS", null, responseBody, durationMs);
//    }
//
//    public void logFailure(WorkflowStep step, Map<String, Object> context, String errorMessage, long durationMs) {
//        saveLog(step, context, "FAILURE", errorMessage, null, durationMs);
//    }
//
//    private void saveLog(WorkflowStep step, Map<String, Object> context, String status, String errorMessage, String responseBody, long durationMs) {
//        try {
//            AuditLog auditLog = AuditLog.builder()
//                    .workflowId(step.getWorkflowConfig().getId())
//                    .stepId(step.getId())
//                    .status(status)
//                    .errorMessage(errorMessage)
//                    .requestContext(objectMapper.writeValueAsString(context))
//                    .responseBody(responseBody)
//                    .executionTimeMs(durationMs)
//                    .createdAt(LocalDateTime.now())
//                    .build();
//            auditLogRepository.save(auditLog);
//        } catch (Exception ex) {
//            // Best effort logging
//            ex.printStackTrace();
//        }
//    }
//}
