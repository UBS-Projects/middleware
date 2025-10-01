package com.middleware;

import com.middleware.service.ConfigService;
import org.apache.camel.Producer;
import org.apache.camel.Consumer;
import org.apache.camel.Processor;
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
        syntax = "config:code",
        category = { Category.CORE },
        producerOnly = true
)
public class ConfigEndpoint extends DefaultEndpoint {

    @UriPath(description = "The code identifying the configuration group.")
    @Metadata(required = true)
    private String code;

    private ConfigService configService;

    public ConfigEndpoint(String uri, ConfigComponent component, String code) {
        super(uri, component);
        this.code = code;
    }

    @Override
    public Producer createProducer() throws Exception {
        return new ConfigProducer(this, configService, code);
    }

    @Override
    public Consumer createConsumer(Processor processor) throws Exception {
        throw new UnsupportedOperationException("Config component is producer only.");
    }

    @Override
    public boolean isSingleton() {
        return true;
    }
}
