package com.middleware.backend.orchestration;

import java.util.List;
import java.util.Map;

public class WorkflowResult {
    
    private boolean success;
    private Map<String, Object> finalContext;
    private List<Map<String, Object>> auditTrail;

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean Success) {
        this.success = Success;
    }

    public Map<String, Object> getFinalContext() {
        return finalContext;
    }

    public void setFinalContext(Map<String, Object> FinalContext) {
        this.finalContext = FinalContext;
    }

    public List<Map<String, Object>> getAuditTrail() {
        return auditTrail;
    }

    public void setAuditTrail(List<Map<String, Object>> AuditTrail) {
        this.auditTrail = AuditTrail;
    }

     

}
