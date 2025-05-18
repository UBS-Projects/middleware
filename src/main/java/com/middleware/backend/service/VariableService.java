package com.middleware.backend.service;

import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class VariableService {

    private final ExpressionParser parser = new SpelExpressionParser();

    /**
     * Replaces {{variable}} placeholders in a BaseUri string using provided variables.
     * @param templateString The BaseUri template with placeholders like {{variable}}
     * @param variables Map of variable names and their values
     * @return Resolved BaseUri string with variables replaced
     */
    public String replaceVariables(String templateString, Map<String, Object> variables) {
        if (templateString == null || templateString.isEmpty() || variables == null) {
            return templateString;
        }

        // Wrap variables in SpEL context
        StandardEvaluationContext context = new StandardEvaluationContext();
        context.setVariables(variables);

        // Replace all {{variable}} occurrences
        String resolved = templateString;
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            String placeholder = String.format("{{%s}}", entry.getKey());
            if (resolved.contains(placeholder)) {
                String value = String.valueOf(entry.getValue());
                resolved = resolved.replace(placeholder, value);
            }
        }

        // Optionally support SpEL expressions within {{ }}
        resolved = resolved.replaceAll("\\{\\{(.*?)\\}\\}", "#{$1}");
        String finalResult = parser.parseExpression('"' + resolved + '"').getValue(context, String.class);

        return finalResult;
    }
}
