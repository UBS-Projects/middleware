package com.middleware;

import com.middleware.service.IntegratedSystemBridgeService;
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
 * <p>
 * Kaoto exposes connection metadata (system type, protocol, host, port, auth, extras)
 * so designers can see the same fields used by the Integrated System admin model.
 * At runtime the producer still loads the stored configuration by {@code code}
 * so existing HTTP/HTTPS routes keep working.
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
     * High-level system category (HTTP API, database, broker, email, file transfer).
     */
    @UriParam(
            displayName = "System Type",
            enums = "HTTP_API,DATABASE,MESSAGE_BROKER,EMAIL,FILE_TRANSFER",
            description = "The type of integrated system (HTTP API, database, message broker, email, or file transfer)."
    )
    private String systemType;

    /**
     * Wire protocol used to connect to the system.
     */
    @UriParam(
            displayName = "Protocol",
            enums = "HTTP,HTTPS,POSTGRESQL,MYSQL,SQLSERVER,ORACLE,SMTP,FTP,SFTP,MQTT,AMQP",
            description = "The communication protocol used by the integrated system."
    )
    private String protocol;

    /**
     * Host address (IP or domain) of the system.
     */
    @UriParam(displayName = "Host", description = "Host address (IP or domain) of the integrated system.")
    private String host;

    /**
     * Port of the system.
     */
    @UriParam(displayName = "Port", description = "Port of the integrated system.")
    private String port;

    /**
     * Authentication type used to access the system.
     */
    @UriParam(
            displayName = "Authentication Type",
            enums = "NONE,BASIC,JWT",
            description = "Authentication type used to access the integrated system."
    )
    private String authenticationType;

    /**
     * The system code identifying the integrated system.
     */
    @UriParam(displayName = "Code", description = "The system code identifying the integrated system.")
    private String code;

    /**
     * Optional additional key 1.
     */
    @UriParam(displayName = "Additional Key 1", description = "Optional additional configuration key 1.")
    private String additionalKey1;

    /**
     * Optional additional value 1.
     */
    @UriParam(displayName = "Additional Value 1", description = "Optional additional configuration value 1.")
    private String additionalValue1;

    /**
     * Optional additional key 2.
     */
    @UriParam(displayName = "Additional Key 2", description = "Optional additional configuration key 2.")
    private String additionalKey2;

    /**
     * Optional additional value 2.
     */
    @UriParam(displayName = "Additional Value 2", description = "Optional additional configuration value 2.")
    private String additionalValue2;

    /**
     * Optional human-readable description.
     */
    @UriParam(displayName = "Description", description = "Optional human-readable description of the integrated system.")
    private String description;

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

    public String getSystemType() {
        return systemType;
    }

    public void setSystemType(String systemType) {
        this.systemType = systemType;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public String getAuthenticationType() {
        return authenticationType;
    }

    public void setAuthenticationType(String authenticationType) {
        this.authenticationType = authenticationType;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getAdditionalKey1() {
        return additionalKey1;
    }

    public void setAdditionalKey1(String additionalKey1) {
        this.additionalKey1 = additionalKey1;
    }

    public String getAdditionalValue1() {
        return additionalValue1;
    }

    public void setAdditionalValue1(String additionalValue1) {
        this.additionalValue1 = additionalValue1;
    }

    public String getAdditionalKey2() {
        return additionalKey2;
    }

    public void setAdditionalKey2(String additionalKey2) {
        this.additionalKey2 = additionalKey2;
    }

    public String getAdditionalValue2() {
        return additionalValue2;
    }

    public void setAdditionalValue2(String additionalValue2) {
        this.additionalValue2 = additionalValue2;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public IntegratedSystemBridgeService getSystemService() {
        return systemService;
    }

    public void setSystemService(IntegratedSystemBridgeService systemService) {
        this.systemService = systemService;
    }
}
