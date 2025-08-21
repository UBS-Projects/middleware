package com.middleware.component.errormapper;

import org.apache.camel.Exchange;
import org.apache.camel.support.DefaultProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The producer for the MyCustom component. It logs a message based on the
 * endpoint configuration.
 */
public class MyCustomProducer extends DefaultProducer {

    private static final Logger LOG = LoggerFactory.getLogger(MyCustomProducer.class);
    private MyCustomEndpoint endpoint;

    public MyCustomProducer(MyCustomEndpoint endpoint) {
        super(endpoint);
        this.endpoint = endpoint;
        LOG.info("MyCustomProducer initialized for endpoint: {}", endpoint.getEndpointUri());
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        String messageToLog;

        // Prioritize message from exchange body, then endpoint, then component default
        if (exchange.getIn().getBody() != null) {
            messageToLog = exchange.getIn().getBody(String.class);
            LOG.debug("Using message from exchange body: {}", messageToLog);
        } else if (endpoint.getMessage() != null && !endpoint.getMessage().isEmpty()) {
            messageToLog = endpoint.getMessage();
            LOG.debug("Using message from endpoint URI: {}", messageToLog);
        } else if (endpoint.getDefaultMessage() != null && !endpoint.getDefaultMessage().isEmpty()) {
            messageToLog = endpoint.getDefaultMessage();
            LOG.debug("Using default message from component: {}", messageToLog);
        } else {
            messageToLog = "No message provided.";
            LOG.warn("No message found, logging default placeholder.");
        }

        // Add prefix if configured
        if (endpoint.getPrefix() != null && !endpoint.getPrefix().isEmpty()) {
            messageToLog = endpoint.getPrefix() + " " + messageToLog;
        }

        // Log the message based on the specified level
        switch (endpoint.getLevel().toUpperCase()) {
        case "INFO":
            LOG.info(messageToLog);
            break;
        case "WARN":
            LOG.warn(messageToLog);
            break;
        case "ERROR":
            LOG.error(messageToLog);
            break;
        default:
            LOG.info("Unknown log level, defaulting to INFO: {}", messageToLog);
        }
    }
}
