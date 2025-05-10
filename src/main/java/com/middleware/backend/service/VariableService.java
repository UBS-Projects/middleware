package com.middleware.backend.service;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

@Service
public class VariableService {

    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{(.*?)\\}\\}");

    public  String replaceVariables(String input, Map<String, Object> variables) {
        if (input == null || variables == null || variables.isEmpty()) {
            return input;
        }

        Matcher matcher = VARIABLE_PATTERN.matcher(input);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String key = matcher.group(1).trim();
            Object value = variables.getOrDefault(key, ""); // if variable not found, replace with empty string
            matcher.appendReplacement(result, Matcher.quoteReplacement(String.valueOf(value)));
        }

        matcher.appendTail(result);
        return result.toString();
    }
}
