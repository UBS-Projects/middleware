package com.middleware.backend.example;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;
@Component
@ConfigurationProperties(prefix = "external-api")
public class ExternalApiProperties {
    private String url;
    private Map<String, String> transformation;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Map<String, String> getTransformation() {
        return transformation;
    }

    public void setTransformation(Map<String, String> transformation) {
        this.transformation = transformation;
    }
}
