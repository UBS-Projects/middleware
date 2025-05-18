package com.middleware.backend.util;

import org.springframework.expression.ExpressionParser;
import org.springframework.expression.common.TemplateParserContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

@Component
public class ApplyTemplate2 {

    public Object applySpelTemplate(String template, Object result) {
        if (template == null || template.trim().isEmpty()) {
            return result;
        }

        ExpressionParser parser = new SpelExpressionParser();
        StandardEvaluationContext context = new StandardEvaluationContext();

        // Flatten the map if needed
        if (result instanceof java.util.Map<?, ?> map) {
            for (var entry : map.entrySet()) {
                context.setVariable(entry.getKey().toString(), entry.getValue());
            }
        } else {
           
            context.setVariable("variables", result); // fallback if not a map
        }
        SpelContextDebugger.printVariables(context);

        return parser.parseExpression(template, new TemplateParserContext()).getValue(context, Object.class);
    }
}
