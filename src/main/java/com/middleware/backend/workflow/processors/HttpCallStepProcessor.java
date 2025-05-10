package com.middleware.backend.workflow.processors;

import org.apache.camel.Exchange;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.middleware.backend.model.WorkflowStep;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class HttpCallStepProcessor implements StepProcessor {

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public void process(Exchange exchange, WorkflowStep step) {
        //String url = step.getConfig().get("url"); // Example: step config has URL
      //  String method = step.getConfig().getOrDefault("method", "GET");

        String url = step.getDestinationApi().getBaseUri(); // Example: step config has URL
        String method = step.getDestinationApi().getHttpMethod(); //.getOrDefault("method", "GET");

        log.info("Executing HTTP step to {}", url);

        String response;
        if ("POST".equalsIgnoreCase(method)) {
            response = restTemplate.postForObject(url, exchange.getIn().getBody(), String.class);
        } else {
            response = restTemplate.getForObject(url, String.class);
        }

        exchange.getIn().setBody(response);
    }
}
