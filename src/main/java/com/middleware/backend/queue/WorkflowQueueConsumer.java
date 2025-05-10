package com.middleware.backend.queue;


import java.util.Map;

import org.springframework.amqp.rabbit.annotation.RabbitListener;

//@Component
public class WorkflowQueueConsumer {
    @RabbitListener(queues = "workflow.queue")
    public void consume(Map<String, Object> message) {
        // This is automatically picked up by Camel Route
    }
}