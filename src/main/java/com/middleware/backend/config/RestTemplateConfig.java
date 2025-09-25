//package com.middleware.backend.config;
//
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.web.client.RestTemplate;
//
///**
// * Configuration class for creating and managing a {@link RestTemplate} bean.
// * This class also defines properties for configuring the base URL for REST calls.
// */
//@Configuration
//public class RestTemplateConfig {
//
//    @Value("${camel.rest.host}")
//    private String host;
//
//    @Value("${camel.rest.port}")
//    private int port;
//
//    @Value("${camel.rest.context-path}")
//    private String contextPath;
//
//    /**
//     * Creates a singleton {@link RestTemplate} bean.
//     * This bean can be injected into other components to make HTTP requests.
//     *
//     * @return A new instance of {@link RestTemplate}.
//     */
//    @Bean
//    public RestTemplate restTemplate() {
//        return new RestTemplate();
//    }
//
//    /**
//     * Constructs and provides the base URL for the Camel REST services.
//     * The URL is built from properties defined in the application's configuration.
//     *
//     * @return The base URL as a string.
//     */
//    @Bean
//    public String getBaseUrl() {
//        return "http://" + host + ":" + port + contextPath;
//    }
//
//}
package com.middleware.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import lombok.extern.slf4j.Slf4j;

import javax.net.ssl.*;
import java.io.IOException;
import java.security.cert.X509Certificate;
import java.net.HttpURLConnection;

/**
 * Configuration class for creating and managing a {@link RestTemplate} bean.
 * This class also defines properties for configuring the base URL for REST calls
 * and handles SSL configuration for DHIS2 connections.
 */
@Configuration
@Slf4j
public class RestTemplateConfig {

    @Value("${camel.rest.host}")
    private String host;

    @Value("${camel.rest.port}")
    private int port;

    @Value("${camel.rest.context-path}")
    private String contextPath;

    @Value("${dhis2.timeout:30000}")
    private int dhis2Timeout;

    @Value("${dhis2.connect-timeout:10000}")
    private int dhis2ConnectTimeout;

    /**
     * Creates a singleton {@link RestTemplate} bean with SSL configuration.
     * This bean can be injected into other components to make HTTP requests.
     *
     * @return A configured instance of {@link RestTemplate}.
     */
    @Bean
    public RestTemplate restTemplate() {
        log.info("Creating RestTemplate with SSL trust-all configuration");

        // ✅ تعطيل فحص SSL للتطوير
        disableSslVerification();

        // Create RestTemplate with custom request factory
        RestTemplate restTemplate = new RestTemplate();

        // Configure timeouts
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory() {
            @Override
            protected void prepareConnection(HttpURLConnection connection, String httpMethod) throws IOException {
                super.prepareConnection(connection, httpMethod);

                // Set timeouts
                connection.setConnectTimeout(dhis2ConnectTimeout);
                connection.setReadTimeout(dhis2Timeout);

                // ✅ للـHTTPS connections، طبق إعدادات SSL
                if (connection instanceof HttpsURLConnection) {
                    HttpsURLConnection httpsConnection = (HttpsURLConnection) connection;
                    httpsConnection.setHostnameVerifier((hostname, session) -> {
                        log.debug("Accepting hostname: {}", hostname);
                        return true; // Accept all hostnames
                    });
                }
            }
        };

        factory.setConnectTimeout(dhis2ConnectTimeout);
        factory.setReadTimeout(dhis2Timeout);
        restTemplate.setRequestFactory(factory);

        log.info("RestTemplate configured with connect-timeout: {}ms, read-timeout: {}ms",
                dhis2ConnectTimeout, dhis2Timeout);

        return restTemplate;
    }

    /**
     * Constructs and provides the base URL for the Camel REST services.
     * The URL is built from properties defined in the application's configuration.
     *
     * @return The base URL as a string.
     */
    @Bean
    public String getBaseUrl() {
        return "http://" + host + ":" + port + contextPath;
    }

    /**
     * ✅ تعطيل فحص شهادات SSL للتطوير
     * تحذير: لا تستخدم هذا في الإنتاج!
     */
    private void disableSslVerification() {
        try {
            log.warn("⚠️  DISABLING SSL VERIFICATION - FOR DEVELOPMENT ONLY!");

            // Create a trust manager that accepts all certificates
            TrustManager[] trustAllCerts = new TrustManager[] {
                    new X509TrustManager() {
                        public X509Certificate[] getAcceptedIssuers() {
                            return new X509Certificate[0];
                        }
                        public void checkClientTrusted(X509Certificate[] certs, String authType) {
                            // Trust all client certificates
                        }
                        public void checkServerTrusted(X509Certificate[] certs, String authType) {
                            // Trust all server certificates
                        }
                    }
            };

            // Install the all-trusting trust manager
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

            // Install the all-trusting host verifier
            HostnameVerifier allHostsValid = (hostname, session) -> {
                log.debug("SSL: Accepting hostname {}", hostname);
                return true;
            };
            HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);

            log.info("✅ SSL verification disabled successfully");

        } catch (Exception e) {
            log.error("❌ Failed to disable SSL verification: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to configure SSL settings", e);
        }
    }
}