package com.middleware.backend.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
public class ExternalOrderApiClient {
    private static final Logger log = LoggerFactory.getLogger(ExternalOrderApiClient.class);

    private final RestTemplate restTemplate;
    private final ExternalApiRepository apiRepo;
    private final ExpressionParser parser = new SpelExpressionParser();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ConstantRepository constantRepository;

    public ExternalOrderApiClient(RestTemplate restTemplate, ExternalApiRepository apiRepo, ConstantRepository constantRepository) {
        this.restTemplate = restTemplate;
        this.apiRepo = apiRepo;
        this.constantRepository = constantRepository;
    }

    public boolean submitOrder(OrderInputItem inputItem, String apiName) {
        Optional<ExternalApi> apiOpt = apiRepo.findByName(apiName);
        if (apiOpt.isEmpty()) {
            System.err.println("❌ API not found: " + apiName);
            return false;
        }

        ExternalApi api = apiOpt.get();
        String template = api.getRequestTemplate();

        try {
            System.out.println(template);

            String evaluatedJson = evaluateTemplate(template, inputItem);

            // ✅ Log the final evaluated payload
            System.out.println("🚀 Final JSON payload to " + api.getUrl() + ":");
            System.out.println(evaluatedJson);

            Map<String, Object> payloadMap = objectMapper.readValue(evaluatedJson, Map.class);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("User-Agent", "Mozilla/5.0"); // Required by ReqBin

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payloadMap, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(api.getUrl(), request, Map.class);

            return "true".equals(String.valueOf(response.getBody().get("success")));
        } catch (Exception e) {
            System.err.println("❌ Error during transformation or external call: " + e.getMessage());
            return false;
        }
    }

    private String evaluateTemplate(String template, Object contextObject) {
        Pattern pattern = Pattern.compile("#\\{([^}]+)}"); // matches #{...}
        Matcher matcher = pattern.matcher(template);
        StringBuffer result = new StringBuffer();
        StandardEvaluationContext context = new StandardEvaluationContext(contextObject);

// Load constants from DB and register as a map
        Map<String, Object> constantsMap = constantRepository.findAll().stream()
                .collect(Collectors.toMap(Constant::getKey, c -> parseConstantValue(c.getValue())));

        context.setVariable("constants", constantsMap);

        while (matcher.find()) {
            String expression = matcher.group(1);
            Object value = parser.parseExpression(expression).getValue(context);

            // ✅ Serialize the value safely to JSON using ObjectMapper
            String replacement;
            try {
                replacement = objectMapper.writeValueAsString(value); // ensures correct string/number/null quoting
            } catch (Exception e) {
                replacement = "null"; // fallback
            }

            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);
        return result.toString();
    }
    private Object parseConstantValue(String val) {
        try {
            if (val.contains(".")) return Double.parseDouble(val);
            return Integer.parseInt(val);
        } catch (NumberFormatException e) {
            return val; // return as string if not numeric
        }
    }

}