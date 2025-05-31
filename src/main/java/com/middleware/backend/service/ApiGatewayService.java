package com.middleware.backend.service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import org.apache.camel.ProducerTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestMethod;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.middleware.backend.dto.ApiResponse;
import com.middleware.backend.exception.ApiNotFoundException;
import com.middleware.backend.exception.InvalidRequestException;
import com.middleware.backend.logging.dto.MiddlewareApiCallLogDto;
import com.middleware.backend.logging.service.MiddlewareApiCallLogService;
import com.middleware.backend.model.ApiEndpoint;
import com.middleware.backend.orchestration.WorkflowOrchestratorRoute;
import com.middleware.backend.orchestration.WorkflowResult;
import com.middleware.backend.repository.ApiEndpointRepository;
import com.middleware.backend.util.ApplyTemplate;
import com.middleware.backend.util.JsonSchemaValidatorUtil;
import com.middleware.backend.util.RequestValidator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApiGatewayService {

    private final ApiEndpointRepository apiEndpointRepository;
    private final ObjectMapper objectMapper;
    private final WorkflowExecutionService workflowExecutionService;
    private final RequestValidator requestValidator;
    private final ApplyTemplate applyTemplate;
    private final WorkflowOrchestratorRoute workflowOrchestratorRoute;
    private final ProducerTemplate producerTemplate;
    private final JsonSchemaValidatorUtil jsonSchemaValidatorUtil;
    private final SpelTemplateEvaluatorService spelTemplateEvaluatorService;
    private final MiddlewareApiCallLogService middlewareApiCallLogService;

    public ApiResponse handleRequest(String path, RequestMethod method, Map<String, Object> requestBody,
            Map<String, String> headers) {
        String transactionId = UUID.randomUUID().toString();
        LocalDateTime receivedAt = LocalDateTime.now();

        MiddlewareApiCallLogDto logDto = MiddlewareApiCallLogDto.builder().transactionId(transactionId)
                .requestMethod(method.name()).requestUri(path).requestHeaders(toJsonSafe(headers))
                .requestBody(toJsonSafe(requestBody)).receivedAt(receivedAt)
                .clientIp(headers.getOrDefault("X-Forwarded-For", "unknown")).build();

        ApiEndpoint endpoint = apiEndpointRepository.findByEndpointPathAndMethod(path, method.name());
        if (endpoint == null) {
            logDto.setResponseCode(404);
            logDto.setErrorMessage("Endpoint not found");
            logDto.setCompletedAt(LocalDateTime.now());
            logDto.setDurationMs(duration(receivedAt, logDto.getCompletedAt()));
            middlewareApiCallLogService.createTransaction(logDto); // Async save
            throw new ApiNotFoundException("Endpoint not found for path: " + path + " and method: " + method.name());
        }
        logDto.setApiEndpointId(endpoint.getId());
        if (endpoint.getTriggerWorkflow() != null) {
            logDto.setWorkflowId(endpoint.getTriggerWorkflow().getId());
        }

        JsonNode jsonBody;
        try {
            jsonBody = objectMapper.valueToTree(requestBody);
        } catch (Exception e) {
            logDto.setResponseCode(400);
            logDto.setErrorMessage("Invalid JSON body");
            logDto.setCompletedAt(LocalDateTime.now());
            logDto.setDurationMs(duration(receivedAt, logDto.getCompletedAt()));
            middlewareApiCallLogService.createTransaction(logDto);
            throw new InvalidRequestException("Invalid JSON body " + requestBody);
        }

        if (!validateInput(endpoint.getInputTemplate(), requestBody)) {
            logDto.setResponseCode(400);
            logDto.setErrorMessage("Body validation failed");
            logDto.setCompletedAt(LocalDateTime.now());
            logDto.setDurationMs(duration(receivedAt, logDto.getCompletedAt()));
            middlewareApiCallLogService.createTransaction(logDto);
            throw new InvalidRequestException("Body validation failed");
        } else {
            log.debug("Valid JsonBody {} against template {}", jsonBody, endpoint.getInputTemplate());
        }

        try {
            Map<String, Object> workflowResult = workflowOrchestratorRoute
                    .executeWorkflow(endpoint.getTriggerWorkflow().getId(), requestBody, headers);

            Map<String, Object> outputTemplate = endpoint.getOutputTemplate();
            Map<String, Object> context = Map.of("variables", workflowResult);
            Object finalBody = spelTemplateEvaluatorService.evaluateTemplate(outputTemplate, context);

            WorkflowResult workflowResult1 = (WorkflowResult) workflowResult.get("workflowResults");

            ApiResponse apiResponse = new ApiResponse();
            apiResponse.setStatus(workflowResult1.isSuccess() ? HttpStatus.OK : HttpStatus.INTERNAL_SERVER_ERROR);
            apiResponse.setBody(finalBody);

            logDto.setResponseCode(apiResponse.getStatus().value());
            logDto.setResponseBody(toJsonSafe((Map<String, Object>) apiResponse.getBody()));
            logDto.setCompletedAt(LocalDateTime.now());
            logDto.setDurationMs(duration(receivedAt, logDto.getCompletedAt()));
            middlewareApiCallLogService.createTransaction(logDto);

            return apiResponse;
        } catch (Exception e) {
            logDto.setResponseCode(500);
            logDto.setErrorMessage("Workflow execution failed: " + e.getMessage());
            logDto.setCompletedAt(LocalDateTime.now());
            logDto.setDurationMs(duration(receivedAt, logDto.getCompletedAt()));
            middlewareApiCallLogService.createTransaction(logDto);

            ApiResponse errorResponse = new ApiResponse();
            errorResponse.setStatus(HttpStatus.INTERNAL_SERVER_ERROR);
            errorResponse.setBody("Workflow execution failed: " + e.getMessage());
            return errorResponse;
        }
    }

    public void executeWorkflow(Long workflowId) {
        producerTemplate.sendBodyAndHeader("direct:executeWorkflow", null, "workflowId", workflowId);
    }

    public Boolean validateInput(Map<String, Object> inputTemplate, Map<String, Object> input) {
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
