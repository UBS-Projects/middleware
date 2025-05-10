package com.middleware.backend.util;

import org.springframework.expression.ExpressionParser;
import org.springframework.expression.common.TemplateParserContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class SpELTempProcessor {

    private final ExpressionParser parser = new SpelExpressionParser();
    private final TemplateParserContext templateContext = new TemplateParserContext(); // enables #{...} parsing

    public String apply(String template, Map<String, Object> variables) {
        
            if (template == null || template.trim().isEmpty()) {
                return null;
            }
        
            StandardEvaluationContext context = new StandardEvaluationContext();
        
            // Flatten top-level keys into root context
            for (Map.Entry<String, Object> entry : variables.entrySet()) {
                Object value = entry.getValue();
        
                // If nested Map, also flatten one level
                if (value instanceof Map<?, ?> nestedMap) {
                    for (Map.Entry<?, ?> nestedEntry : nestedMap.entrySet()) {
                        if (nestedEntry.getKey() instanceof String) {
                            context.setVariable(nestedEntry.getKey().toString(), nestedEntry.getValue());
                        }
                    }
                }
        
                context.setVariable(entry.getKey(), value); // also set the top-level entry
            }

        
            return parser.parseExpression(template, templateContext).getValue(context, String.class);
        }
        
}
