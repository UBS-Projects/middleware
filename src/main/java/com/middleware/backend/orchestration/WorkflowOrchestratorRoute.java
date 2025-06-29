
package com.middleware.backend.orchestration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

import com.middleware.backend.model.WorkflowStep;
import com.middleware.backend.repository.WorkflowStepRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
@Component
public class WorkflowOrchestratorRoute extends RouteBuilder {

    private final WorkflowStepRepository workflowStepRepository;
    private final StepProcessorRegistry processorRegistry;
    private final ProducerTemplate producerTemplate;

    @Override
    public void configure() {

        from("direct:workflow-route").routeId("workflow-orchestrator")
                .log("Executing workflow for ID: ${header.workflowId}").process(this::loadSteps)
                .process(this::executeSteps).setBody(exchange -> exchange.getProperty("workflowResult"))
                .log("Workflow completed.");

    }

    private void loadSteps(Exchange exchange) {
        Long workflowId = exchange.getIn().getHeader("workflowId", Long.class);
        List<WorkflowStep> steps = workflowStepRepository.findByWorkflowConfigIdOrderByStepOrderAsc(workflowId);

        log.info("Loaded {} steps from database", steps.size());

        Map<Integer, List<WorkflowStep>> groupedSteps = Optional.ofNullable(steps).orElse(List.of()).stream()
                .sequential()
                .collect(Collectors.groupingBy(step -> Optional.ofNullable(step.getStepOrder()).orElse(0)));

        exchange.setProperty("workflowGroupedSteps", groupedSteps);
    }

    private void executeSteps(Exchange exchange) {
        Map<String, Object> inputData = exchange.getIn().getBody(Map.class);
        Map<String, String> headers = exchange.getIn().getHeader("inputHeaders", Map.class);
        Map<String, Object> context = new HashMap<>(inputData);
        List<Map<String, Object>> auditLog = new ArrayList<>();
        exchange.setProperty("requestBody", inputData);
        exchange.setProperty("inputHeaders", headers);

        @SuppressWarnings("unchecked")
        Map<Integer, List<WorkflowStep>> groupedSteps = (Map<Integer, List<WorkflowStep>>) exchange
                .getProperty("workflowGroupedSteps");

        log.info("Grouped Workflow Steps:");
        groupedSteps.forEach((groupId, steps) -> {
            log.info("Group {}:", groupId);
            for (WorkflowStep step : steps) {
                log.info("  - Step: {}", step.getStepOrder(), step.getStepName(), step.getForkGroupId());
            }
        });

        if (groupedSteps == null || groupedSteps.isEmpty()) {
            throw new IllegalStateException("No grouped steps found for workflow execution.");
        }

        int maxGroupId = Collections.max(groupedSteps.keySet());

        for (int i = 0; i <= maxGroupId; i++) {
            List<WorkflowStep> group = groupedSteps.get(i);
            if (group == null)
                continue;

            if (group.size() == 1) {
                WorkflowStep step = group.get(0);
                // Map<String, Object> result = executeStep(step, context, headers);
                Map<String, Object> result = executeStep(step, exchange);
                context.putAll(result);
                auditLog.add(result);
            } else {
                List<Map<String, Object>> results = group.parallelStream().map(step -> executeStep(step, exchange))
                        .collect(Collectors.toList());

                for (Map<String, Object> res : results) {
                    context.putAll(res);
                    auditLog.add(res);
                }
            }
        }

        WorkflowResult result = new WorkflowResult();
        result.setSuccess(true);
        result.setFinalContext(context);
        result.setAuditTrail(auditLog);

        exchange.setProperty("workflowResults", result);
    }

    private Map<String, Object> executeStep(WorkflowStep step, Exchange exchange) {
        StepProcessor processor = processorRegistry.getProcessor(step.getStepType());
        if (processor == null) {
            throw new IllegalStateException("No processor found for stepType: " + step.getStepType());
        }
        return processor.process(step, exchange);
    }

    public Map<String, Object> executeWorkflow(Long workflowId, Map<String, Object> inputData,
            Map<String, String> headers) throws Exception {
        Exchange exchange = producerTemplate.request("direct:workflow-route", ex -> {
            ex.getIn().setHeader("workflowId", workflowId);
            ex.getIn().setHeader("inputHeaders", headers);
            ex.getIn().setBody(inputData);
        });

        if (exchange.getException() != null) {
            throw exchange.getException();
        }

        // return exchange.getMessage().getBody(Object.class);
        return exchange.getAllProperties();
    }
}
