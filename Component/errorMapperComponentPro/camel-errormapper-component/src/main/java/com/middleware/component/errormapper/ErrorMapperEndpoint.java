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

import com.middleware.component.errormapper.service.ErrorMappingService;

import lombok.Getter;
import lombok.Setter;

/**
 * Represents an ErrorMapper endpoint. This component will process a failed REST
 * response and map it to a standardized error structure.
 */
@Getter
@Setter
@UriEndpoint(firstVersion = "1.0.0", scheme = "errormapper", title = "Error Mapper", syntax = "errormapper:operation", category = {
        Category.MESSAGING, Category.TRANSFORMATION }, producerOnly = true)
public class ErrorMapperEndpoint extends DefaultEndpoint {

    private static final Logger LOG = LoggerFactory.getLogger(ErrorMapperEndpoint.class);

    @UriPath
    @Metadata(required = true, description = "The operation or context for error mapping. E.g., 'restFailure'.")
    private String operation;

    @UriParam(defaultValue = "true", description = "Whether to include the original system error message in the mapped response.")
    private boolean includeOriginalMessage = true;

    // The ErrorMappingService is passed from the component
    private ErrorMappingService errorMappingService;

    public ErrorMapperEndpoint(String uri, ErrorMapperComponent component, String operation) {
        super(uri, component);
        this.operation = operation;
        LOG.info("ErrorMapperEndpoint created for URI: {}", uri);
    }

    @Override
    public Producer createProducer() throws Exception {
        LOG.info("Creating ErrorMapperProducer for endpoint: {}", getEndpointUri());
        return new ErrorMapperProducer(this, errorMappingService);
    }

    @Override
    public Consumer createConsumer(Processor processor) throws Exception {
        throw new UnsupportedOperationException(
                "ErrorMapper component is producer only and not intended for consuming messages directly.");
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    public boolean isIncludeOriginalMessage() {
        return includeOriginalMessage;
    }

    /**
     * Sets whether to include the original system error message in the mapped
     * response.
     * 
     * @param includeOriginalMessage True to include, false otherwise.
     */
    public void setIncludeOriginalMessage(boolean includeOriginalMessage) {
        this.includeOriginalMessage = includeOriginalMessage;
        LOG.debug("Include original message set to: {}", includeOriginalMessage);
    }

    public ErrorMappingService getErrorMappingService() {
        return errorMappingService;
    }

    public void setErrorMappingService(ErrorMappingService errorMappingService) {
        this.errorMappingService = errorMappingService;
    }
}
