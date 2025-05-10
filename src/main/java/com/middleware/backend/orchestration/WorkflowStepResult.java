package com.middleware.backend.orchestration;

import java.util.List;
import java.util.Map;

import org.apache.camel.spi.annotations.Component;

import lombok.Data;

@Data
public class WorkflowStepResult {
    
    private boolean success;
    private Map<String, Object> finalContext;
    private List<Map<String, Object>> auditTrail;

   

     

}
