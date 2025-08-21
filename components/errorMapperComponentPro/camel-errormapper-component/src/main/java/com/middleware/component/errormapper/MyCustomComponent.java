package com.middleware.component.errormapper;

import java.util.Map;

import org.apache.camel.Endpoint;
import org.apache.camel.spi.annotations.Component;
import org.apache.camel.support.DefaultComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents the component that manages {@link MyCustomEndpoint}.
 */
@Component("mycustom")
public class MyCustomComponent extends DefaultComponent {

    private static final Logger LOG = LoggerFactory.getLogger(MyCustomComponent.class);

    // This property can be configured globally on the component level
    // For example: mycustom:message=Hello
    private String defaultMessage;

    public MyCustomComponent() {
        LOG.info("MyCustomComponent initialized.");
    }

    @Override
    protected Endpoint createEndpoint(String uri, String remaining, Map<String, Object> parameters) throws Exception {
        MyCustomEndpoint endpoint = new MyCustomEndpoint(uri, this);
        endpoint.setMessage(remaining); // The 'remaining' part of the URI becomes the message
        endpoint.setDefaultMessage(defaultMessage); // Set component-level default message

        // Set properties from URI query parameters
        setProperties(endpoint, parameters);

        LOG.info("Created endpoint: {}", uri);
        return endpoint;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }

    /**
     * Sets a default message for all endpoints created by this component. This is a
     * component-level option.
     * 
     * @param defaultMessage The default message.
     */
    public void setDefaultMessage(String defaultMessage) {
        this.defaultMessage = defaultMessage;
        LOG.info("Default message set on component: {}", defaultMessage);
    }
}
