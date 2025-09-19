package com.middleware.backend.orchestration.processors;

import java.util.HashMap;
import java.util.Map;

import org.apache.camel.Exchange;
import org.springframework.stereotype.Component;

import com.middleware.backend.model.WorkflowStep;
import com.middleware.backend.orchestration.StepProcessor;

@Component("API_CALL11")
public class HttpCallProcessor implements StepProcessor {

    @Override
    public Map<String, Object> process(WorkflowStep step, Exchange exchange) {
        // Actual HTTP logic here
        Map<String, Object> result = new HashMap<>();
        result.put("stepINs", step.getStepName());
        result.put("status", "SUCCESS");
        result.put("output", "Called HTTP for step " + step.getId());
        result.put("name", "myname");
        return result;
    }
}
