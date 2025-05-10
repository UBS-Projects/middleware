package com.middleware.backend.workflow;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class WorkflowScheduler {

    @Scheduled(fixedRate = 60000) // Every 60 seconds
    public void orchestrate() {
        System.out.println("Running workflow orchestration logic...");
        // Add orchestration logic here
    }
}
