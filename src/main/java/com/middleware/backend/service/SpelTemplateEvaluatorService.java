package com.middleware.backend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class SpelTemplateEvaluatorService {

    private final ExpressionParser parser = new SpelExpressionParser();
    private static final Pattern SPEL_PATTERN = Pattern.compile("#\\{\\{(.*?)}}");

    /**
     * Recursively evaluates SpEL placeholders in Strings, Maps, Lists.
     *
     * @param input   Any object (String, Map, List, etc.)
     * @param context Map of variables to resolve placeholders
     * @return Fully evaluated result
     */
    public Object evaluateTemplate(Object input, Map<String, Object> context) {
        StandardEvaluationContext evaluationContext = new StandardEvaluationContext();
        evaluationContext.setVariables(context);

        return evaluateRecursive(input, evaluationContext);
    }

    private Object evaluateRecursive(Object input, StandardEvaluationContext evaluationContext) {
        if (input instanceof String) {
            return evaluateString((String) input, evaluationContext);
        } else if (input instanceof Map) {
            Map<String, Object> result = new LinkedHashMap<>();
            ((Map<?, ?>) input).forEach((key, value) -> {
                result.put(String.valueOf(key), evaluateRecursive(value, evaluationContext));
            });
            return result;
        } else if (input instanceof List) {
            List<Object> result = new ArrayList<>();
            for (Object item : (List<?>) input) {
                result.add(evaluateRecursive(item, evaluationContext));
            }
            return result;
        } else {
            return input;
        }
    }

    private String evaluateString(String template, StandardEvaluationContext evaluationContext) {
        Matcher matcher = SPEL_PATTERN.matcher(template);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String expressionText = matcher.group(1);
            try {
                Expression expression = parser.parseExpression(expressionText);
                Object value = expression.getValue(evaluationContext);
                String resolvedValue = String.valueOf(value);

                log.debug("Evaluating expression: [{}] => [{}]", expressionText, resolvedValue);

                matcher.appendReplacement(sb, Matcher.quoteReplacement(resolvedValue));
            } catch (Exception e) {
                log.warn("Failed to evaluate expression: [{}], keeping original placeholder", expressionText, e);
                matcher.appendReplacement(sb, Matcher.quoteReplacement("#{{" + expressionText + "}}"));
            }
        }

        matcher.appendTail(sb);
        return sb.toString();
    }
}
