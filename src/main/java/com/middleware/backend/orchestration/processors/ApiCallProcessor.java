package com.middleware.backend.orchestration.processors;

import java.util.HashMap;
import java.util.Map;

import org.apache.camel.Exchange;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.middleware.backend.errormapping.service.BackendErrorMappingService;
import com.middleware.backend.model.WorkflowStep;
import com.middleware.backend.orchestration.StepProcessor;
import com.middleware.backend.service.SpelTemplateEvaluatorService;
import com.middleware.backend.service.VariableService;
import com.middleware.backend.util.ApplyTemplate;
import com.middleware.backend.util.SpELTempProcessor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component("API_CALL")
@RequiredArgsConstructor
public class ApiCallProcessor implements StepProcessor {

    private final VariableService variableService;
    private final SpELTempProcessor spELTempProcessor;
    private final RestTemplate restTemplate;
    private final ApplyTemplate applyTemplate;
    private final BackendErrorMappingService errorMappingService;
    @Autowired
    private SpelTemplateEvaluatorService spelTemplateEvaluatorService;

    @Override
    public Map<String, Object> process(WorkflowStep step, Exchange exchange) {
        log.info("Starting API_CALL Processor for step: {}", step.getStepName());

        Map<String, Object> variables = new HashMap<>();
        // variables.put("variables", exchange.getAllProperties());
        variables = exchange.getAllProperties();
        Map<String, Object> context = Map.of("variables", variables);

        log.debug("Received Exchange getAllProperties: {}", context);
        // log.debug("Exchange Headers: {}", exchange.getIn().getHeaders());
        // log.debug("Step Details: {}", step);

        if (step == null || step.getDestinationApi() == null) {
            log.error("Step or DestinationApi not found in exchange properties!");
            throw new IllegalArgumentException("Invalid step configuration!");
        }
        // log.debug("Variables before evaluating apiUrl, input and headers : {}",
        // variables);
        try {
            // Resolve target URL
            String apiUrl = variableService.replaceVariables(step.getDestinationApi().getBaseUri(), variables);
            log.info("Resolved API URL: {}", apiUrl);

            // Prepare request body from template (Map)
            Map<String, Object> inputTemplate = step.getDestinationApi().getInputTemplate();

            Object requestBody = spelTemplateEvaluatorService.evaluateTemplate(inputTemplate, context);
            // Object requestBody = applyTemplate.applySpelTemplate(inputTemplate,
            // variables);
            log.info("Prepared Request Body: {}", requestBody);

            // Prepare request headers
            HttpHeaders httpHeaders = new HttpHeaders();
            // httpHeaders.setContentType(MediaType.APPLICATION_JSON);
            if (step.getDestinationApi().getInputHeaderTemplate() != null) {
                Map<String, Object> headerTemplate = step.getDestinationApi().getInputHeaderTemplate();
                Object resolvedHeaders = spelTemplateEvaluatorService.evaluateTemplate(headerTemplate, context);
                // Object resolvedHeaders = applyTemplate.applySpelTemplate(headerTemplate,
                // variables);

                if (resolvedHeaders instanceof Map<?, ?> resolvedHeaderMap) {
                    resolvedHeaderMap.forEach((k, v) -> httpHeaders.add(k.toString(), v != null ? v.toString() : ""));
                }
                // log.info("Prepared Request Headers: {}", httpHeaders);
            }
            // Handle Authentication
            String authType = step.getDestinationApi().getAuthType();
            Map<String, Object> authCredentials = step.getDestinationApi().getAuthCredentials();

            if ("BASIC".equalsIgnoreCase(authType) && authCredentials != null) {
                String username = authCredentials.getOrDefault("username", "").toString();
                String password = authCredentials.getOrDefault("password", "").toString();
                String basicAuth = username + ":" + password;
                String encodedAuth = java.util.Base64.getEncoder().encodeToString(basicAuth.getBytes());
                httpHeaders.set("Authorization", "Basic " + encodedAuth);
            } else if ("BEARER".equalsIgnoreCase(authType) && authCredentials != null) {
                String token = authCredentials.getOrDefault("token", "").toString();
                httpHeaders.setBearerAuth(token);
            }

            httpHeaders.setContentType(MediaType.APPLICATION_JSON);

            log.info("Prepared Request Headers: {}", httpHeaders);

            HttpMethod httpMethod = HttpMethod.valueOf(step.getDestinationApi().getHttpMethod().toUpperCase());
            log.info("HTTP Method: {}", httpMethod);

            // Execute the actual API call
            HttpEntity<Object> requestEntity = new HttpEntity<>(requestBody, httpHeaders);
            Object apiResponse = restTemplate.exchange(apiUrl, httpMethod, requestEntity, Object.class).getBody();
            log.info("API Response: {}", apiResponse);

            // Assign thisResponse for output processing
            variables.put("rawResponse", apiResponse);
            log.debug("Variables after adding raw response and before evaluating the response: {}", variables);
            // Prepare Output Template (Map)
            Map<String, Object> outputTemplate = step.getDestinationApi().getOutputTemplate();
            Object response = spelTemplateEvaluatorService.evaluateTemplate(outputTemplate, context);
            // Object response = applyTemplate.applySpelTemplate(outputTemplate, variables);
            log.info("formated response: {}", response);

            // Prepare final result map
            Map<String, Object> result = new HashMap<>();
            result.put("stepId", step.getId());
            result.put("status", "SUCCESS");
            result.put("request", requestBody);
            result.put("output", response);
            result.put("rawResponse", apiResponse);

            // Update exchange properties
            exchange.setProperty(step.getStepName(), result);
            log.debug("Updated Exchange Properties after step execution: {}", exchange.getProperties());

            log.info("API_CALL Processor completed successfully for step: {}", step.getStepName());
            return result;

        } catch (Exception e) {
            log.error("Error during API_CALL Processor execution for step {}: {}", step.getStepName(), e.getMessage(),
                    e);

            // Perform Dynamic Error Mapping
            // Map<String, Object> mappedError =
            // errorMappingService.mapError(step.getDestinationApi(), e.getMessage());

            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("stepId", step.getId());
            errorResult.put("status", "FAILED");
            errorResult.put("Techerror", e.getMessage());
            // errorResult.put("error", mappedError);

            // Update exchange with failure result
            exchange.setProperty(step.getStepName(), errorResult);

            return errorResult;
        }
    }
}
