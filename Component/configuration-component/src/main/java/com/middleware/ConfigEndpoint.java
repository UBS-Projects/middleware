package com.middleware;

import com.middleware.service.ConfigService;
import org.apache.camel.Producer;
import org.apache.camel.Consumer;
import org.apache.camel.Processor;
import org.apache.camel.spi.UriParam;
import org.apache.camel.support.DefaultEndpoint;
import lombok.Getter;
import lombok.Setter;
import org.apache.camel.spi.UriEndpoint;
import org.apache.camel.spi.UriPath;
import org.apache.camel.Category;
import org.apache.camel.spi.Metadata;

@Getter
@Setter
@UriEndpoint(
        firstVersion = "1.0.0",
        scheme = "config",
        title = "Config",
        syntax = "config",
        category = { Category.MESSAGING },
        producerOnly = true
)
public class ConfigEndpoint extends DefaultEndpoint {

    @UriPath(description = "Dummy path required by Camel. Not used at runtime.")
    private String operation;

    @UriParam(description = "The code identifying the configuration group.")
    private String code;


    private ConfigService configService;

    public ConfigEndpoint(String uri, ConfigComponent component) {
        super(uri, component);
    }

    @Override
    public Producer createProducer() throws Exception {
        return new ConfigProducer(this, configService);
    }

    @Override
    public Consumer createConsumer(Processor processor) throws Exception {
        throw new UnsupportedOperationException("Config component is producer only.");
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

    public ConfigService getConfigService() {
        return configService;
    }

    public void setConfigService(ConfigService configService) {
        this.configService = configService;
    }
}
