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
@UriEndpoint(
        firstVersion = "1.0.0",
        scheme = "integratedSystem",
        title = "integratedSystem",
        syntax = "integratedSystem",
        category = {Category.MESSAGING},
        producerOnly = true
)
public class IntegratedSystemEndpoint extends DefaultEndpoint {

    @UriPath(description = "Dummy operation path for Camel syntax.")
    private String operation;

    @UriParam(description = "The system code identifying the integrated system.")
    private String code;

    private IntegratedSystemBridgeService systemService;

    public IntegratedSystemEndpoint(String uri, IntegratedSystemComponent component) {
        super(uri, component);
    }

    @Override
    public Producer createProducer() throws Exception {
        return new IntegratedSystemProducer(this, systemService);
    }

    @Override
    public Consumer createConsumer(Processor processor) throws Exception {
        throw new UnsupportedOperationException("IntegratedSystem component is producer-only.");
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

    public IntegratedSystemBridgeService getSystemService() {
        return systemService;
    }

    public void setSystemService(IntegratedSystemBridgeService systemService) {
        this.systemService = systemService;
    }
}
