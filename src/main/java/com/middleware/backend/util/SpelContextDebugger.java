package com.middleware.backend.util;

import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.lang.reflect.Field;
import java.util.Map;

public class SpelContextDebugger {

    @SuppressWarnings("unchecked")
    public static void printVariables(StandardEvaluationContext context) {
        try {
            Field variablesField = StandardEvaluationContext.class.getDeclaredField("variables");
            variablesField.setAccessible(true);
            Map<String, Object> variables = (Map<String, Object>) variablesField.get(context);
            System.out.println("=== Context Variables ===");
            for (Map.Entry<String, Object> entry : variables.entrySet()) {
                System.out.println(entry.getKey() + " = " + entry.getValue());
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Could not access context variables");
        }
    }
}
