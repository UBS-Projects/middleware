package com.middleware.backend.orchestration.processors;

import com.middleware.backend.model.WorkflowStep;
import com.middleware.backend.orchestration.StepProcessor;
import com.middleware.backend.service.SpelTemplateEvaluatorService;
import com.middleware.backend.util.ApplyTemplate;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.apache.camel.Exchange;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component("TRANSFORMATION")
@RequiredArgsConstructor
public class TransformationProcessor implements StepProcessor {

    private final ExpressionParser parser = new SpelExpressionParser();
    private final ApplyTemplate applyTemplate;
    private SpelTemplateEvaluatorService spelTemplateEvaluatorService;

    @Override
    public Map<String, Object> process(WorkflowStep step, Exchange exchange) {
        log.info("Executing TRANSFORMATION step: {}", step.getStepName());
        // Map<String, Object> context = exchange.getIn().getBody(Map.class);
        // Map<String, String> headers = exchange.getIn().getHeader("inputHeaders",
        // Map.class);

        Map<String, Object> variables = new HashMap<>();
        variables.put("variables", exchange.getAllProperties());
        Map<String, Object> context = Map.of("variables", variables);
        Object result;

        log.debug("Received variables: ");
        log.debug("Context variables: {}", context);
        // log.debug("Header variables: {}", headers);

        // Map<String, Object> result = new HashMap<>(context); // Preserve the original
        // context

        Map<String, Object> expressionStr = step.getTransformationExpression();
        if (expressionStr == null || expressionStr.isEmpty()) {
            log.warn("No transformation expression found for step: {}", step.getId());
            throw new IllegalArgumentException("Invalid step configuration!");
        }

        try {
            /*
             * StandardEvaluationContext evaluationContext = new
             * StandardEvaluationContext(); evaluationContext.setVariable("variables",
             * variables); //evaluationContext.setVariable("headers", headers);
             * 
             * 
             * Expression expression = parser.parseExpression(expressionStr); Object value =
             * expression.getValue(evaluationContext);
             */

            result = spelTemplateEvaluatorService.evaluateTemplate(expressionStr, context);
            // Add result under a default key or determine based on use-case
            variables.put("thisResponse", result);

            log.info("Transformation result for step '{}': {}", step.getStepName(), result);
        } catch (Exception e) {
            log.error("Error evaluating transformation expression for step {}: {}", step.getId(), e.getMessage(), e);
            Map<String, Object> resultFinal = new HashMap<>();
            resultFinal.put("stepId", step.getId());
            resultFinal.put("status", "FAILED");
            resultFinal.put("output", e.getMessage());
exchange.setProperty(step.getStepName(), resultFinal);
            throw new RuntimeException("Transformation failed for step ID " + step.getId(), e);

        }

        Map<String, Object> resultFinal = new HashMap<>();
        resultFinal.put("stepId", step.getId());
        resultFinal.put("status", "SUCCESS");
        resultFinal.put("expression", expressionStr);
        resultFinal.put("output", result);

        exchange.setProperty(step.getStepName(), resultFinal);

        return resultFinal;
    }
}
