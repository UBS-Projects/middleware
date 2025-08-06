package com.middleware.backend.orchestration;

import org.springframework.stereotype.Component;

import com.middleware.backend.model.WorkflowStep;
import com.middleware.backend.workflow.processors.HttpCallStepProcessor;
import com.middleware.backend.workflow.processors.StepProcessor;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class StepProcessorFactory {

    private final HttpCallStepProcessor httpCallStepProcessor;

    public StepProcessor getProcessor(WorkflowStep step) {
        switch (step.getStepType()) {
            case "HTTP_CALL":
                return httpCallStepProcessor;
            // Add other cases for DB_CALL, SCRIPT_EXEC, etc.
            default:
                throw new IllegalArgumentException("Unsupported step type: " + step.getStepType());
        }
    }
}
