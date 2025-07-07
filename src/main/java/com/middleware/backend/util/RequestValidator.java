package com.middleware.backend.util;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.camel.Exchange;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.middleware.backend.exception.ApiNotFoundException;
import com.middleware.backend.logging.dto.MiddlewareApiCallLogDto;
import com.middleware.backend.logging.service.MiddlewareApiCallLogService;
import com.middleware.backend.model.ApiEndpoint;
import com.middleware.backend.repository.ApiEndpointRepository;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component("RequestValidator")
@RequiredArgsConstructor
public class RequestValidator {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final JsonSchemaFactory schemaFactory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
    private final ApiEndpointRepository apiEndpointRepository;
    private final JsonSchemaValidatorUtil jsonSchemaValidatorUtil;
    private final MiddlewareApiCallLogService middlewareApiCallLogService;

    public boolean validateJson(String schemaStr, JsonNode data) {
        try {
            JsonSchema schema = schemaFactory.getSchema(schemaStr);
            Set<ValidationMessage> errors = schema.validate(data);
            return errors.isEmpty();
        } catch (Exception e) {
            return false;
        }

    }

    public void validateJsonBody(Exchange exchange) throws RuntimeException {

        String body = exchange.getIn().getBody(String.class);
        String method = exchange.getIn().getHeader(Exchange.HTTP_METHOD, String.class);
        String path = exchange.getIn().getHeader("HTTP_PATH", String.class);
        Map<String, Object> headers = exchange.getIn().getHeaders();
        // path = path.replace("/camel", "");

        log.debug("Exchange {}", exchange);
        log.debug("recived Body {} \n method {}\npath {}", body, method, path);
        String transactionId = UUID.randomUUID().toString();
        LocalDateTime receivedAt = LocalDateTime.now();
        String clientIp = exchange.getIn().getHeader("X-Forwarded-For", String.class);
        if (clientIp == null) {
            clientIp = "unknown";
        }
        MiddlewareApiCallLogDto dto = MiddlewareApiCallLogDto.builder().clientIp(clientIp).build();

        MiddlewareApiCallLogDto logDto = MiddlewareApiCallLogDto.builder().transactionId(transactionId)
                .requestMethod(method).requestUri(path).requestHeaders(toJsonSafe(headers))
                .requestBody(toJsonSafe(body)).receivedAt(receivedAt).clientIp(clientIp).build();

        ApiEndpoint endpoint = apiEndpointRepository.findByEndpointPathAndMethod(path, method);
        if (endpoint == null) {
            logDto.setResponseCode(404);
            logDto.setErrorMessage("Endpoint not found");
            logDto.setCompletedAt(LocalDateTime.now());
            logDto.setDurationMs(duration(receivedAt, logDto.getCompletedAt()));
            middlewareApiCallLogService.createTransaction(logDto); // Async save
            throw new ApiNotFoundException("Endpoint not found for path: " + path + " and method: " + method);
        }
        logDto.setApiEndpointId(endpoint.getId());
        if (endpoint.getTriggerWorkflow() != null) {
            logDto.setWorkflowId(endpoint.getTriggerWorkflow().getId());
        }

        JsonNode jsonBody;
        try {
            jsonBody = objectMapper.valueToTree(body);
        } catch (Exception e) {
            logDto.setResponseCode(400);
            logDto.setErrorMessage("Invalid JSON body");
            logDto.setCompletedAt(LocalDateTime.now());
            logDto.setDurationMs(duration(receivedAt, logDto.getCompletedAt()));
            middlewareApiCallLogService.createTransaction(logDto);
            throw new RuntimeException("Invalid JSON body " + body);
        }

        if (!validateInput(endpoint.getInputTemplate(), body)) {
            logDto.setResponseCode(400);
            logDto.setErrorMessage("Body validation failed");
            logDto.setCompletedAt(LocalDateTime.now());
            logDto.setDurationMs(duration(receivedAt, logDto.getCompletedAt()));
            middlewareApiCallLogService.createTransaction(logDto);
            throw new RuntimeException("Body validation failed");

        } else {
            log.debug("Valid JsonBody {} against template {}", jsonBody, endpoint.getInputTemplate());

        }

    }

    public boolean validateHeaders(String headerTemplateJson, Map<String, String> actualHeaders) {
        try {
            JsonNode requiredHeaders = objectMapper.readTree(headerTemplateJson);
            for (JsonNode key : requiredHeaders) {
                if (!actualHeaders.containsKey(key.asText())) {
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public Boolean validateInput(Map<String, Object> inputTemplate, String input) {
        try {
            String schemaString = objectMapper.writeValueAsString(inputTemplate);
            String inputString = objectMapper.writeValueAsString(input);
            return jsonSchemaValidatorUtil.validate(schemaString, inputString);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to process JSON", e);
        }
    }

    private long duration(LocalDateTime start, LocalDateTime end) {
        return java.time.Duration.between(start, end).toMillis();
    }

    private String toJsonSafe(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "{}";
        }
    }
}
