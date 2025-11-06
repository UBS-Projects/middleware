package com.middleware.component;

import com.middleware.component.service.ErrorMappingBridgeService;
import org.apache.camel.Category;
import org.apache.camel.Consumer;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.apache.camel.spi.UriEndpoint;
import org.apache.camel.spi.UriParam;
import org.apache.camel.spi.UriPath;
import org.apache.camel.support.DefaultEndpoint;

/**
 * Endpoint for applying error mapping based on an existing mapping ID.
 */
@UriEndpoint(
        firstVersion = "1.0.0",
        scheme = "errormappingcomponent",
        title = "errormappingcomponent",
        syntax = "errormappingcomponent",
        category = {Category.MESSAGING},
        producerOnly = true
)
public class ErrorMappingEndpoint extends DefaultEndpoint {

    @UriPath(description = "Dummy operation path for Camel syntax.")
    private String operation;

    @UriParam(description = "The ID of Error Mapping")
    private String code;

    private ErrorMappingBridgeService bridgeService;

    public ErrorMappingEndpoint(String uri, ErrorMappingComponent component) {
        super(uri, component);
    }

    @Override
    public Producer createProducer() throws Exception {
        return new ErrorMappingProducer(this, bridgeService);
    }

    @Override
    public Consumer createConsumer(Processor processor) {
        throw new UnsupportedOperationException("ErrorMapping component is producer-only.");
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
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