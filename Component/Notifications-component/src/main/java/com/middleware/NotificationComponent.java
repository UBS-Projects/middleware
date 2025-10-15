package com.middleware;

import com.middleware.service.NotificationServiceBridge;
import org.apache.camel.CamelContext;
import org.apache.camel.Endpoint;
import org.apache.camel.spi.Metadata;
import org.apache.camel.spi.annotations.Component;
import org.apache.camel.support.DefaultComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Camel component for notification integration.
 * Creates endpoints and manages the bridge service for sending notifications.
 */
@Component("notification")
public class NotificationComponent extends DefaultComponent {

    private static final Logger LOG = LoggerFactory.getLogger(NotificationComponent.class);

    /**
     * Bridge service for sending notifications.
     */
    @Metadata(label = "internal")
    private NotificationServiceBridge notificationService;

    /**
     * Default constructor.
     */
    public NotificationComponent() {
        LOG.info("NotificationComponent initialized.");
    }

    /**
     * Constructor with Camel context.
     *
     * @param context the CamelContext
     */
    public NotificationComponent(CamelContext context) {
        super(context);
        LOG.info("NotificationComponent initialized with CamelContext.");
    }

    /**
     * Creates a notification endpoint for the component.
     *
     * @param uri        the endpoint URI
     * @param remaining  unused part of the URI
     * @param parameters endpoint parameters
     * @return the created endpoint
     * @throws Exception if the notification service is not found or an error occurs
     */
    @Override
    protected Endpoint createEndpoint(String uri, String remaining, Map<String, Object> parameters) throws Exception {
        NotificationEndpoint endpoint = new NotificationEndpoint(uri, this);
        setProperties(endpoint, parameters);

        if (this.notificationService == null) {
            this.notificationService = getCamelContext().getRegistry()
                    .lookupByNameAndType(NotificationServiceBridge.BEAN_ID, NotificationServiceBridge.class);
            if (this.notificationService == null) {
                throw new IllegalStateException("NotificationServiceBridge is required but not found.");
            }
        }

        endpoint.setNotificationService(notificationService);
        return endpoint;
    }
}