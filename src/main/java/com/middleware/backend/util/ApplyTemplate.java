package com.middleware.backend.util;

import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class ApplyTemplate {

    private final ExpressionParser parser = new SpelExpressionParser();

    public Object applySpelTemplate(Map<String, Object> template, Map<String, Object> inputData) {
        if (template == null || inputData == null) {
            return null;
        }

        Map<String, Object> resolvedMap = new HashMap<>();

        // Set context root to inputData["variables"]
              Object variables = inputData.get("variables");
        if (variables== null )
         variables = inputData;
         
        StandardEvaluationContext context = new StandardEvaluationContext(variables);

        System.out.println("==== Evaluation Context Root (variables) ====");
        System.out.println(variables);

        // Process each entry
        for (Map.Entry<String, Object> entry : template.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            if (value instanceof String strVal && strVal.contains("#{{")) {
                String expression = extractExpression(strVal);
                System.out.println("Evaluating SpEL: " + expression);

                Object evaluated = evaluateExpression(expression, context, strVal);
                resolvedMap.put(key, evaluated);
            } else if (value instanceof Map<?, ?> nestedMap) {
                resolvedMap.put(key, applySpelTemplate((Map<String, Object>) nestedMap, inputData));
            } else {
                resolvedMap.put(key, value);
            }
        }

        return resolvedMap;
    }

    private String extractExpression(String strVal) {
        return strVal.replace("#{{", "").replace("}}", "").trim();
    }

    private Object evaluateExpression(String expression, StandardEvaluationContext context, String originalValue) {
        try {
            return parser.parseExpression(expression).getValue(context);
        } catch (Exception e) {
            System.err.println("Failed to evaluate expression: " + expression + " | Error: " + e.getMessage());
            return originalValue;
        }
    }
}
