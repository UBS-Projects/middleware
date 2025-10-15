package com.middleware;

import com.middleware.service.IntegratedSystemBridgeService;
import lombok.Data;
import org.apache.camel.Category;
import org.apache.camel.Consumer;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.apache.camel.spi.UriEndpoint;
import org.apache.camel.spi.UriParam;
import org.apache.camel.spi.UriPath;
import org.apache.camel.support.DefaultEndpoint;

/**
 * Camel endpoint for interacting with integrated systems.
 * This endpoint acts as a producer for fetching system configurations.
 */
@UriEndpoint(
        firstVersion = "1.0.0",
        scheme = "integratedSystem",
        title = "integratedSystem",
        syntax = "integratedSystem",
        category = {Category.MESSAGING},
        producerOnly = true
)
public class IntegratedSystemEndpoint extends DefaultEndpoint {

    /**
     * Dummy operation path for Camel syntax.
     */
    @UriPath(description = "Dummy operation path for Camel syntax.")
    private String operation;

    /**
     * The system code identifying the integrated system.
     */
    @UriParam(description = "The system code identifying the integrated system.")
    private String code;

    /**
     * Bridge service for fetching integrated system configuration.
     */
    private IntegratedSystemBridgeService systemService;

    /**
     * Constructs a new IntegratedSystemEndpoint.
     *
     * @param uri       the endpoint URI
     * @param component the parent component
     */
    public IntegratedSystemEndpoint(String uri, IntegratedSystemComponent component) {
        super(uri, component);
    }

    /**
     * Creates a producer for this endpoint.
     *
     * @return the producer
     * @throws Exception if an error occurs
     */
    @Override
    public Producer createProducer() throws Exception {
        return new IntegratedSystemProducer(this, systemService);
    }

    /**
     * Throws UnsupportedOperationException since this endpoint is producer-only.
     *
     * @param processor processor
     * @return nothing; always throws exception
     * @throws Exception always thrown
     */
    @Override
    public Consumer createConsumer(Processor processor) throws Exception {
        throw new UnsupportedOperationException("IntegratedSystem component is producer-only.");
    }

    /**
     * Indicates that this endpoint is singleton.
     *
     * @return true
     */
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

    public IntegratedSystemBridgeService getSystemService() {
        return systemService;
    }

    public void setSystemService(IntegratedSystemBridgeService systemService) {
        this.systemService = systemService;
    }
}