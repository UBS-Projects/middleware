package com.middleware;

import com.middleware.service.NotificationServiceBridge;
import org.apache.camel.Consumer;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.apache.camel.spi.Metadata;
import org.apache.camel.spi.UriEndpoint;
import org.apache.camel.spi.UriParam;
import org.apache.camel.spi.UriPath;
import org.apache.camel.support.DefaultEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.camel.Category;

import java.util.List;

@UriEndpoint(
        firstVersion = "1.0.0",
        scheme = "notification",
        syntax = "notification",
        title = "Notification",
        category = { Category.MESSAGING },
        producerOnly = true
)
public class NotificationEndpoint extends DefaultEndpoint {

    @UriPath(description = "Dummy path required by Camel. Not used at runtime.")
    private String operation;

    // Make these configurable in Kaoto
    @UriParam(description = "Target group codes")
    private List<String> groupCodes;

    @UriParam(description = "Template code for the notification")
    private String templateCode;

    @UriParam(description = "Channel code for the notification")
    private String channelCode;

    @Metadata(label = "internal")
    private NotificationServiceBridge notificationService;


    private static final Logger LOG = LoggerFactory.getLogger(NotificationEndpoint.class);



    public NotificationEndpoint(String uri, NotificationComponent component) {
        super(uri, component);
        LOG.info("NotificationEndpoint created for URI: {}", uri);
    }

    @Override
    public Producer createProducer() throws Exception {
        LOG.info("Creating NotificationProducer for endpoint: {}", getEndpointUri());
        return new NotificationProducer(this, notificationService);
    }

    @Override
    public Consumer createConsumer(Processor processor) throws Exception {
        throw new UnsupportedOperationException("Notification component is producer-only.");
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    // internal setter only
    void setNotificationService(NotificationServiceBridge notificationService) {
        this.notificationService = notificationService;
    }

    public List<String> getGroupCodes() { return groupCodes; }
    public void setGroupCodes(List<String> groupCodes) { this.groupCodes = groupCodes; }

    public String getTemplateCode() { return templateCode; }
    public void setTemplateCode(String templateCode) { this.templateCode = templateCode; }

    public String getChannelCode() { return channelCode; }
    public void setChannelCode(String channelCode) { this.channelCode = channelCode; }
}
