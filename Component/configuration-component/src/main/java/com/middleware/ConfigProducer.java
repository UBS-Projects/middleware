package com.middleware;

import com.middleware.model.ConfigDetail;
import com.middleware.service.ConfigService;
import org.apache.camel.Exchange;
import org.apache.camel.support.DefaultProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Producer implementation for the Config component.
 * <p>
 * This producer retrieves configuration data from the {@link ConfigService} based on
 * a configuration code. The code can be provided either through:
 * <ul>
 *   <li>The message body as a {@link ConfigDetail} object</li>
 *   <li>The endpoint's code parameter</li>
 * </ul>
 * </p>
 * <p>
 * The retrieved configuration values are set as headers in the exchange message,
 * using the configuration code as the header name.
 * </p>
 *
 * <h3>Error Handling</h3>
 * <p>
 * If no configuration is found for the specified code, the producer:
 * <ul>
 *   <li>Sets HTTP response code to 404</li>
 *   <li>Sets an error header to true</li>
 *   <li>Sets an error message in the response body</li>
 *   <li>Stops route processing</li>
 * </ul>
 * </p>
 *
 * @author middleware
 * @version 1.0.0
 * @since 1.0.0
 * @see ConfigEndpoint
 * @see ConfigComponent
 * @see ConfigService
 */
public class ConfigProducer extends DefaultProducer {

    private static final Logger LOG = LoggerFactory.getLogger(ConfigProducer.class);

    /**
     * The endpoint associated with this producer.
     */
    private final ConfigEndpoint endpoint;

    /**
     * The configuration service used to retrieve configuration data.
     */
    private final ConfigService configService;

    /**
     * Constructs a ConfigProducer with the specified endpoint and service.
     *
     * @param endpoint the ConfigEndpoint that created this producer
     * @param configService the service used to retrieve configuration data
     */
    public ConfigProducer(ConfigEndpoint endpoint, ConfigService configService) {
        super(endpoint);
        this.endpoint = endpoint;
        this.configService = configService;
    }

    /**
     * Processes the exchange by retrieving configuration data.
     * <p>
     * This method performs the following steps:
     * <ol>
     *   <li>Extracts the configuration code from the message body or endpoint parameter</li>
     *   <li>Retrieves the configuration using the {@link ConfigService}</li>
     *   <li>If found, sets each configuration key-value pair as a header in the exchange</li>
     *   <li>If not found, sets an error response with HTTP 404 status</li>
     * </ol>
     * </p>
     *
     * @param exchange the Camel exchange containing the request and response messages
     * @throws Exception if an error occurs during configuration retrieval
     */
    @Override
    public void process(Exchange exchange) throws Exception {
        ConfigDetail detail = exchange.getIn().getBody(ConfigDetail.class);
        if (detail == null) {
            detail = new ConfigDetail();
            detail.setCode(endpoint.getCode());
        }
        ConfigDetail result = configService.getConfig (detail.getCode());
        if (result == null) {
            String code = exchange.getIn().getHeader("code", String.class);
            String message = "No configuration found for code: " + (code != null ? code : "UNKNOWN");

            exchange.getMessage().setBody(message);
            exchange.getMessage().setHeader(Exchange.HTTP_RESPONSE_CODE, 404);
            exchange.getMessage().setHeader("error", true);

            // ✅ Mark the exchange as handled
            exchange.setProperty(Exchange.ROUTE_STOP, true);
            exchange.setProperty(Exchange.EXCEPTION_HANDLED, true);

            return;
        }
        LOG.info("Config Result Loaded: {}", result.getConfigs());

        // Set the fetched config as the response body
        result.getConfigs().forEach((key, value) -> {
            exchange.getMessage().setHeader(result.getCode(), value);
        });
    }
}
