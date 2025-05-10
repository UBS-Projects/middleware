package com.middleware.backend.orchestration.processors;

import com.middleware.backend.model.WorkflowStep;
import com.middleware.backend.orchestration.StepProcessor;
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
public class TransformationProcessor implements StepProcessor {

    private final ExpressionParser parser = new SpelExpressionParser();

    @Override
    public Map<String, Object> process(WorkflowStep step,Exchange exchange) {
        log.info("Executing TRANSFORMATION step: {}", step.getStepName());
        Map<String, Object> context = exchange.getIn().getBody(Map.class);
        Map<String, String> headers = exchange.getIn().getHeader("inputHeaders", Map.class);
        
        log.debug("Received variables: ");
        log.debug("Context variables: {}", context);
        log.debug("Header variables: {}", headers);

        Map<String, Object> result = new HashMap<>(context); // Preserve the original context

        String expressionStr = step.getTransformationExpression();
        if (expressionStr == null || expressionStr.isEmpty()) {
            log.warn("No transformation expression found for step: {}", step.getId());
            return result;
        }

        try {
            StandardEvaluationContext evaluationContext = new StandardEvaluationContext();
            evaluationContext.setVariable("context", exchange);
            //evaluationContext.setVariable("headers", headers);

            Expression expression = parser.parseExpression(expressionStr);
            Object value = expression.getValue(evaluationContext);

            // Add result under a default key or determine based on use-case
            result.put("transformationResult", value);

            log.info("Transformation result for step '{}': {}", step.getStepName(), value);
        } catch (Exception e) {
            log.error("Error evaluating transformation expression for step {}: {}", step.getId(), e.getMessage(), e);
            throw new RuntimeException("Transformation failed for step ID " + step.getId(), e);
        }

        return result;
    }
}
