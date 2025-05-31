package com.middleware.backend.logging.controller;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.middleware.backend.logging.dto.WorkflowStepLogDto;
import com.middleware.backend.logging.mapper.WorkflowStepLogMapper;
import com.middleware.backend.logging.model.WorkflowStepLog;
import com.middleware.backend.logging.repository.WorkflowStepLogRepository;

import jakarta.persistence.criteria.Predicate;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/workflow-step-logs")
@RequiredArgsConstructor
public class WorkflowStepLogController {

    private final WorkflowStepLogRepository repository;
    private final WorkflowStepLogMapper mapper;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getAll(@RequestParam Map<String, String> filters,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size,
            @RequestParam(value = "sort", defaultValue = "id,desc") String sortParam) {

        Specification<WorkflowStepLog> spec = buildSpecification(filters);

        List<Sort.Order> orders = Arrays.stream(sortParam.split(";")).map(item -> {
            String[] parts = item.split(",");
            return new Sort.Order(parts.length > 1 ? Sort.Direction.fromString(parts[1]) : Sort.Direction.ASC,
                    parts[0]);
        }).collect(Collectors.toList());

        Pageable pageable = PageRequest.of(page, size, Sort.by(orders));
        Page<WorkflowStepLog> pageLogs = repository.findAll(spec, pageable);

        List<WorkflowStepLogDto> dtos = pageLogs.getContent().stream().map(mapper::toDto).collect(Collectors.toList());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("content", dtos);
        response.put("currentPage", pageLogs.getNumber());
        response.put("totalItems", pageLogs.getTotalElements());
        response.put("totalPages", pageLogs.getTotalPages());

        return ResponseEntity.ok(response);
    }

    @GetMapping(value = "/export", produces = "text/csv")
    public void exportToCsv(@RequestParam Map<String, String> filters, HttpServletResponse response)
            throws IOException {
        Specification<WorkflowStepLog> spec = buildSpecification(filters);
        List<WorkflowStepLog> logs = repository.findAll(spec);

        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; filename=workflow_step_logs.csv");

        PrintWriter writer = response.getWriter();

        // CSV Header
        writer.println(
                "ID,Transaction ID,Workflow Execution ID,Step ID,Execution Status,Retry Count,Started At,Ended At");

        for (WorkflowStepLog log : logs) {
            writer.printf("%d,%s,%d,%d,%s,%d,%s,%s\n", log.getId(), safe(log.getTransactionId()),
                    log.getWorkflowExecutionId(), log.getStepId(), safe(log.getExecutionStatus()),
                    Optional.ofNullable(log.getRetryCount()).orElse(0),
                    Optional.ofNullable(log.getStartedAt()).map(LocalDateTime::toString).orElse(""),
                    Optional.ofNullable(log.getEndedAt()).map(LocalDateTime::toString).orElse(""));
        }

        writer.flush();
        writer.close();
    }

    private Specification<WorkflowStepLog> buildSpecification(Map<String, String> filters) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            filters.forEach((key, value) -> {
                try {
                    switch (key) {
                    case "executionStatus" -> predicates.add(cb.equal(root.get("executionStatus"), value));
                    case "workflowExecutionId", "stepId" -> predicates
                            .add(cb.equal(root.get(key), Long.parseLong(value)));
                    case "retryCountMin" -> predicates.add(cb.ge(root.get("retryCount"), Integer.parseInt(value)));
                    case "retryCountMax" -> predicates.add(cb.le(root.get("retryCount"), Integer.parseInt(value)));
                    case "startedAtFrom" -> predicates
                            .add(cb.greaterThanOrEqualTo(root.get("startedAt"), LocalDateTime.parse(value)));
                    case "startedAtTo" -> predicates
                            .add(cb.lessThanOrEqualTo(root.get("startedAt"), LocalDateTime.parse(value)));
                    case "transactionId" -> predicates.add(cb.equal(root.get("transactionId"), value));
                    }
                } catch (Exception ignored) {
                }
            });

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private String safe(String value) {
        if (value == null)
            return "";
        return value.replaceAll("\"", "\"\"").replaceAll(",", " ");
    }
}
