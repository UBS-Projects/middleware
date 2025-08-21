package com.middleware.component.errormapper;

import org.apache.camel.Category;
import org.apache.camel.Consumer;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.apache.camel.spi.Metadata;
import org.apache.camel.spi.UriEndpoint;
import org.apache.camel.spi.UriParam;
import org.apache.camel.spi.UriPath;
import org.apache.camel.support.DefaultEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a MyCustom endpoint. This component will log a message to the
 * console.
 */
@UriEndpoint(firstVersion = "1.0.0", scheme = "mycustom", title = "My Custom Component", syntax = "mycustom:message", category = {
        Category.TRANSFORMATION }, producerOnly = true) // producerOnly as it logs out
public class MyCustomEndpoint extends DefaultEndpoint {

    private static final Logger LOG = LoggerFactory.getLogger(MyCustomEndpoint.class);

    @UriPath
    @Metadata(required = true)
    private String message; // The message from the URI path

    @UriParam(defaultValue = "INFO", enums = "INFO,WARN,ERROR")
    private String level = "INFO"; // Log level, configurable via URI parameter

    @UriParam(label = "advanced", description = "An optional prefix for the logged message.")
    private String prefix; // A custom prefix, configurable via URI parameter

    // This property is inherited from the component, but can be overridden on the
    // endpoint
    private String defaultMessage;

    public MyCustomEndpoint(String uri, MyCustomComponent component) {
        super(uri, component);
        LOG.info("MyCustomEndpoint created for URI: {}", uri);
    }

    @Override
    public Producer createProducer() throws Exception {
        LOG.info("Creating MyCustomProducer for endpoint: {}", getEndpointUri());
        return new MyCustomProducer(this);
    }

    @Override
    public Consumer createConsumer(Processor processor) throws Exception {
        // This component is producer-only based on @UriEndpoint(producerOnly = true)
        // If you need a consumer, you would implement MyCustomConsumer and remove
        // producerOnly = true
        throw new UnsupportedOperationException("MyCustom component is producer only.");
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    public String getMessage() {
        return message;
    }

    /**
     * Sets the main message to be logged by the component. This value comes from
     * the URI path.
     * 
     * @param message The message.
     */
    public void setMessage(String message) {
        this.message = message;
        LOG.debug("Endpoint message set to: {}", message);
    }

    public String getLevel() {
        return level;
    }

    /**
     * Sets the log level for the message (INFO, WARN, ERROR).
     * 
     * @param level The log level.
     */
    public void setLevel(String level) {
        this.level = level;
        LOG.debug("Endpoint log level set to: {}", level);
    }

    public String getPrefix() {
        return prefix;
    }

    /**
     * Sets an optional prefix for the logged message.
     * 
     * @param prefix The prefix.
     */
    public void setPrefix(String prefix) {
        this.prefix = prefix;
        LOG.debug("Endpoint prefix set to: {}", prefix);
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }

    /**
     * Sets the default message from the component. This allows overriding if needed
     * on the endpoint.
     * 
     * @param defaultMessage The default message.
     */
    public void setDefaultMessage(String defaultMessage) {
        this.defaultMessage = defaultMessage;
        LOG.debug("Endpoint inherited default message: {}", defaultMessage);
    }
}
