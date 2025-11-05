package com.middleware.component.errormapper;

import com.middleware.component.errormapper.service.ErrorMappingBridgeService;
import org.apache.camel.Category;
import org.apache.camel.Consumer;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.apache.camel.spi.Metadata;
import org.apache.camel.spi.UriEndpoint;
import org.apache.camel.spi.UriParam;
import org.apache.camel.spi.UriPath;
import org.apache.camel.support.DefaultEndpoint;

/**
 * Endpoint for applying error mapping based on an existing mapping ID.
 */
@UriEndpoint(
        firstVersion = "1.0.0",
        scheme = "errorMapping",
        title = "Error Mapping",
        syntax = "errorMapping:code",
        category = {Category.TRANSFORMATION, Category.MESSAGING},
        producerOnly = true
)
public class ErrorMappingEndpoint extends DefaultEndpoint {

    @UriPath
    @Metadata(required = true, description = "The error mapping ID to fetch and apply")
    private String code;

    private ErrorMappingBridgeService bridgeService;

    public ErrorMappingEndpoint(String uri, ErrorMappingComponent component) {
        super(uri, component);
    }

    @Override
    public Producer createProducer() throws Exception {
        if (bridgeService == null) {
            throw new IllegalStateException("ErrorMappingBridgeService not set");
        }
        return new ErrorMappingProducer(this, bridgeService);
    }

    @Override
    public Consumer createConsumer(Processor processor) {
        throw new UnsupportedOperationException("ErrorMapping component is producer-only.");
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public ErrorMappingBridgeService getBridgeService() {
        return bridgeService;
    }

    public void setBridgeService(ErrorMappingBridgeService bridgeService) {
        this.bridgeService = bridgeService;
    }
}