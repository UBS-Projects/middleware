package com.middleware.backend.workflow.processors;

import org.apache.camel.Exchange;
import com.middleware.backend.model.WorkflowStep;

public interface StepProcessor {
    void process(Exchange exchange, WorkflowStep step) throws Exception;
}
