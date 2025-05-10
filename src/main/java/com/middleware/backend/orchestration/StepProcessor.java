package com.middleware.backend.orchestration;

import java.util.Map;

import org.apache.camel.Exchange;

import com.middleware.backend.model.WorkflowStep;

public interface StepProcessor {
    Map<String, Object> process(WorkflowStep step, Exchange exchange);
}
