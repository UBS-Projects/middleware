package com.middleware.backend.queue;

import java.util.HashMap;
import java.util.Map;

//import org.springframework.amqp.rabbit.core.RabbitTemplate;

import lombok.RequiredArgsConstructor;

//@Service
@RequiredArgsConstructor
public class WorkflowQueuePublisher {
//    private final RabbitTemplate rabbitTemplate;

    public void sendWorkflowExecution(Long workflowId, Map<String, Object> variables) {
        Map<String, Object> message = new HashMap<>();
        message.put("workflowId", workflowId);
        message.put("variables", variables);
//        rabbitTemplate.convertAndSend("workflow.queue", message);
    }
}
