package com.middleware.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.middleware.backend.dto.ApiResponse;
import com.middleware.backend.exception.ApiNotFoundException;
import com.middleware.backend.exception.InvalidRequestException;
import com.middleware.backend.model.ApiEndpoint;
import com.middleware.backend.orchestration.WorkflowOrchestratorRoute;
import com.middleware.backend.repository.ApiEndpointRepository;
import com.middleware.backend.util.ApplyTemplate;
import com.middleware.backend.util.RequestValidator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.apache.camel.ProducerTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.HashMap;
import java.util.Map;

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

    public ApiResponse handleRequest(String path, RequestMethod method, Map<String, Object> requestBody, Map<String, String> headers) {
        ApiEndpoint endpoint = apiEndpointRepository.findByEndpointPathAndMethod(path, method.name());
        if (endpoint == null) {
            throw new ApiNotFoundException("Endpoint not found for path: " + path + " and method: " + method.name());
        }
    
        JsonNode jsonBody;
        try {
            jsonBody = objectMapper.valueToTree(requestBody);
        } catch (Exception e) {
            throw new InvalidRequestException("Invalid JSON body " + requestBody);
        }
    
        if (!requestValidator.validateJson(endpoint.getInputTemplate(), jsonBody)) {
            throw new InvalidRequestException("Body validation failed");
        }
    
        try {
            Object workflowResult = workflowOrchestratorRoute.executeWorkflow(endpoint.getTriggerWorkflow().getId(), requestBody, headers);
    
            // Process outputTemplate using SpEL
            String outputTemplate = endpoint.getOutputTemplate();
            Object finalBody = applyTemplate.applySpelTemplate(outputTemplate, workflowResult);
    
            ApiResponse apiResponse = new ApiResponse();
            apiResponse.setStatus(HttpStatus.OK);
            apiResponse.setBody(finalBody);
           
            //apiResponse.setHeaders(Map.of("X-Workflow-ID", String.valueOf(endpoint.getTriggerWorkflow().getId())));
            return apiResponse;
    
        } catch (Exception e) {
            ApiResponse errorResponse = new ApiResponse();
            errorResponse.setStatus(HttpStatus.INTERNAL_SERVER_ERROR);
            errorResponse.setBody("Workflow execution failed: " + e.getMessage());
            return errorResponse;
        }
    }
    

    public void executeWorkflow(Long workflowId) {
        producerTemplate.sendBodyAndHeader("direct:executeWorkflow", null, "workflowId", workflowId);
    }
}
