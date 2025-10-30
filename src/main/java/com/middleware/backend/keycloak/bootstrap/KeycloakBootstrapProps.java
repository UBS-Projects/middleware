package com.middleware.backend.keycloak.bootstrap;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import java.util.List;

@Data
@Configuration
@ConfigurationProperties(prefix = "keycloak.bootstrap")
public class KeycloakBootstrapProps {
    private String serverUrl;
    private String adminRealm;
    private String adminUsername;
    private String adminPassword;
    private String targetRealm;
    private List<String> frontendRedirects;
}
