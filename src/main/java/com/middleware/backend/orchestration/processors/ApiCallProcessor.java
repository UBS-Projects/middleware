package com.middleware.backend.orchestration.processors;

import java.util.HashMap;
import java.util.Map;

import org.apache.camel.Exchange;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.middleware.backend.model.WorkflowStep;
import com.middleware.backend.orchestration.StepProcessor;
import com.middleware.backend.service.VariableService;
import com.middleware.backend.util.ApplyTemplate;
import com.middleware.backend.util.SpELTempProcessor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component("API_CALL1")
@RequiredArgsConstructor
public class ApiCallProcessor implements StepProcessor {

    private final VariableService variableService;
    private final SpELTempProcessor spELTempProcessor;
    private final RestTemplate restTemplate = new RestTemplate(); // or inject via @Bean
    private final ApplyTemplate applyTemplate;

    @Override
    public Map<String, Object> process(WorkflowStep step, Exchange exchange) {
       // WorkflowStep step = exchange.getProperty("workflowStep", WorkflowStep.class);
       // Map<String, Object> variables = exchange.getProperty("workflowVariables", Map.class);

       Map<String, Object> variables = exchange.getAllProperties();
       //variables.put("requestbody",exchange.getIn().getBody(Map.class) );
       
       Map<String, String> headers = exchange.getIn().getHeader("inputHeaders", Map.class);
      // Map<String,object> callResponse = new HashMap<>();

        if (step == null ) {
            throw new IllegalArgumentException("Step not found in exchange properties!");
        }
        log.info("Executing API_CALL step: {}", step.getStepName());
        log.debug("Received variables: ");
        log.debug("Context variables: {}", variables);
        log.debug("Header variables: {}", headers);
        // Resolve target URL and body with variables
        
        String apiUrl = variableService.replaceVariables(step.getDestinationApi().getBaseUri(), variables);
       // String template = "#{'{userId:'''+ #requestbody['userid'].toString() + ''', name:'''+ #requestbody['name'] + '''}'}";
       String template = "#{'{userId:'''+ #variables['requestbody']['userid'] + ''', name:'''+ #variables['requestbody']['name'] + '''}'}";


        

        log.debug("Template : {}",template);
Map<String,Object> var =new HashMap<>();
var.put("variables" ,"{" + 
        "  requestbody = { name = \"tstname\", userid = '25', amount = 50 }," + 
        "  name = \"tstname\"," +
        "  userid = 25" );
        log.debug("Variables : {}",var);
        Object res = applyTemplate.applySpelTemplate(template, var);
        log.debug("Transformed res: {}",res);

        step.getDestinationApi().setInputTemplate(template);
        log.debug("Dsit input template: {}",step.getDestinationApi().getInputTemplate());
        log.debug("Dsit Variabeles: {}",variables);
        Object requestBody = spELTempProcessor.apply(step.getDestinationApi().getInputTemplate(), variables);
        variables.put("thisrequest", requestBody);
        log.debug("Transformed input: {}",requestBody);
        // Prepare headers
        HttpHeaders httpHeaders = new HttpHeaders();
        if (step.getDestinationApi().getInputHeaderTemplate() != null) {
            step.getDestinationApi().getInputHeaderTemplate().forEach(httpHeaders::add);
        }
        HttpMethod httpMethod = HttpMethod.valueOf(step.getDestinationApi().getHttpMethod().toUpperCase());


        //HttpEntity<String> requestEntity = new HttpEntity<>(requestBody, httpHeaders);

        // Perform the actual API call
        //ResponseEntity<String> response = restTemplate.exchange(apiUrl, httpMethod, requestEntity, String.class);

        Map<String,Object>  body =new HashMap<>();
        body.put("userid",90000);
        body.put("orderid",15);
        body.put("name","Mohammad");
log.debug("manipulated outpu body",body);
variables.put("thisresponse", body);
        // Apply output template to response
       // variables.put("response", response.getBody());
       // variables.put("response", body);
       log.debug("Variables before eval output: {}", variables);
       Object output = spELTempProcessor.apply(step.getDestinationApi().getOutputTemplate(), variables);        
        //String output = variableService.replaceVariables(step.getDestinationApi().getOutputTemplate(), variables);
       // log.debug("Received variables: ");
       // log.debug("Context variables: {}", variables);
       // log.debug("Header variables: {}", headers);
        // Set processed response on exchange
        //exchange.getMessage().setBody(output);
        Map<String, Object> result = new HashMap<>();
        result.put("stepId", step.getId());
        result.put("status", "SUCCESS");
        result.put("request", requestBody);
        result.put("output", output);
        
        exchange.setProperty(step.getStepName(), result);
        log.debug("Exchange infor after step execution variables: {}", exchange.getProperty(step.getStepName()));
        log.debug("Exchange infor after step execution variables: {}", exchange.getAllProperties());
        return result;
    }
}
