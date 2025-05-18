package com.middleware.backend.service;

import java.util.HashMap;
import java.util.Map;

import org.apache.camel.ProducerTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestMethod;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.middleware.backend.dto.ApiResponse;
import com.middleware.backend.exception.ApiNotFoundException;
import com.middleware.backend.exception.InvalidRequestException;
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
@Autowired
private SpelTemplateEvaluatorService spelTemplateEvaluatorService;



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
   
        if (!validateInput(endpoint.getInputTemplate(), requestBody)) {
            throw new InvalidRequestException("Body validation failed");
        }else log.debug("Valid JsonBody {} againest template {}" +jsonBody , endpoint.getInputTemplate());
    
        try {
            Map<String,Object> workflowResult = workflowOrchestratorRoute.executeWorkflow(endpoint.getTriggerWorkflow().getId(), requestBody, headers);
    
            // Process outputTemplate using SpEL
           // String template = "#{ {'userid': #variables['Call User Info API']['output']['userid'], 'name': #variables['Call User Info API']['output']['name'] } }"  ;
            //endpoint.setOutputTemplate(template);
           // Map<String,Object> vars = new HashMap<>();
            //vars.put("variables",workflowResult) ;
            //vars = workflowResult;
            
          
            Map<String,Object> outputTemplate = endpoint.getOutputTemplate();

            Map<String, Object> context = Map.of("variables", workflowResult);
            Object finalBody = spelTemplateEvaluatorService.evaluateTemplate(outputTemplate, context);
            //stepContext.put("response", resolvedResponse);
            //Object finalBody = applyTemplate.applySpelTemplate(outputTemplate, vars);

            WorkflowResult workflowResult1 = (WorkflowResult) workflowResult.get("workflowResults");
    
            ApiResponse apiResponse = new ApiResponse();
            apiResponse.setStatus(workflowResult1.isSuccess()? HttpStatus.OK :HttpStatus.INTERNAL_SERVER_ERROR );
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

    public Boolean validateInput( Map<String, Object> inputTemplate,Map<String, Object> input) {
    try {
        // Convert inputTemplate Map to JSON String
        String schemaString = objectMapper.writeValueAsString(inputTemplate);

        // Convert input payload Map to JSON String
        String inputString = objectMapper.writeValueAsString(input);

        // Validate input JSON against schema JSON
        return jsonSchemaValidatorUtil.validate(schemaString, inputString);

    } catch (JsonProcessingException e) {
        throw new RuntimeException("Failed to process JSON", e);
    }
}
}
