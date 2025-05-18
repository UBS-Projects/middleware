package com.middleware.backend.orchestration.processors;

import com.middleware.backend.model.WorkflowStep;
import com.middleware.backend.orchestration.StepProcessor;
import com.middleware.backend.util.ApplyTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component("DATABASE_FETCH")
@RequiredArgsConstructor
public class DatabaseFetchProcessor implements StepProcessor {

    private final JdbcTemplate jdbcTemplate;
    private final ApplyTemplate applyTemplate;

    @Override
    public Map<String, Object> process(WorkflowStep step, Exchange exchange) {
        log.info("Executing DATABASE_FETCH step: {}", step.getStepName());

        // Extract body context and headers
        Map<String, Object> context = exchange.getIn().getBody(Map.class);
        Map<String, String> headers = exchange.getIn().getHeader("inputHeaders", Map.class);

        // Build variables map for template evaluation
        Map<String, Object> variables = new HashMap<>();
        variables.put("variables", exchange.getAllProperties());
        if (context != null) {
            variables.putAll(context);
        }
        if (headers != null) {
            variables.putAll(headers);
        }

        log.debug("Variables for database fetch: {}", variables);

        // Extract query expression from transformationExpression map
        Map<String, Object> transformationExpression = step.getTransformationExpression();
        if (transformationExpression == null || !transformationExpression.containsKey("query")) {
            log.warn("No 'query' key found in transformationExpression for step: {}", step.getId());
            throw new RuntimeException("Query expression is missing for step ID " + step.getId());
        }

        String queryTemplate = String.valueOf(transformationExpression.get("query"));

        if (queryTemplate.isEmpty()) {
            log.warn("Query template is empty for step: {}", step.getId());
            throw new RuntimeException("Query template is empty for step ID " + step.getId());
        }

        try {
            // Wrap query string into a map to match applySpelTemplate signature
            Map<String, Object> queryTemplateMap = new HashMap<>();
            queryTemplateMap.put("query", queryTemplate);

            // Apply SpEL to the query template
            Object finalQueryObj = applyTemplate.applySpelTemplate(queryTemplateMap, variables);

            String finalQuery = String.valueOf(finalQueryObj);

            log.info("Evaluated SQL query: {}", finalQuery);

            // Execute query and fetch result
            List<Map<String, Object>> queryResult = jdbcTemplate.queryForList(finalQuery);

            log.info("Query result for step '{}': {}", step.getStepName(), queryResult);

            // Prepare final result map
            Map<String, Object> resultFinal = new HashMap<>();
            resultFinal.put("stepId", step.getId());
            resultFinal.put("status", "SUCCESS");
            resultFinal.put("output", queryResult);

            // Set exchange properties for downstream steps
            exchange.setProperty(step.getStepName(), resultFinal);
            exchange.setProperty("thisResponse", queryResult);

            return resultFinal;

        } catch (Exception e) {
            log.error("Error during DATABASE_FETCH for step {}: {}", step.getId(), e.getMessage(), e);
            throw new RuntimeException("Database fetch failed for step ID " + step.getId(), e);
        }
    }
}
