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

@Component("notification")
public class NotificationComponent extends DefaultComponent {

    private static final Logger LOG = LoggerFactory.getLogger(NotificationComponent.class);

    @Metadata(label = "internal")
    private NotificationServiceBridge notificationService;

    public NotificationComponent() {
        LOG.info("NotificationComponent initialized.");
    }

    public NotificationComponent(CamelContext context) {
        super(context);
        LOG.info("NotificationComponent initialized with CamelContext.");
    }

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
