package com.middleware;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.middleware.model.NotificationDetail;
import com.middleware.service.NotificationServiceBridge;
import org.apache.camel.Exchange;
import org.apache.camel.support.DefaultProducer;
import java.util.List;

/**
 * Producer implementation for the NotificationEndpoint.
 * Responsible for sending notifications using the NotificationServiceBridge.
 */
public class NotificationProducer extends DefaultProducer {

    /**
     * The endpoint associated with this producer.
     */
    private final NotificationEndpoint endpoint;

    /**
     * Bridge service for sending notifications.
     */
    private final NotificationServiceBridge notificationService;

    /**
     * Jackson ObjectMapper for potential use in serialization.
     */
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Constructs a new NotificationProducer.
     *
     * @param endpoint            the endpoint
     * @param notificationService the bridge service
     */
    public NotificationProducer(NotificationEndpoint endpoint, NotificationServiceBridge notificationService) {
        super(endpoint);
        this.endpoint = endpoint;
        this.notificationService = notificationService;
    }

    /**
     * Processes the Camel Exchange to send a notification.
     * The notification details are taken from the message body or endpoint parameters.
     *
     * @param exchange the Camel exchange
     * @throws Exception if an error occurs during processing
     */
    @Override
    public void process(Exchange exchange) throws Exception {
        NotificationDetail detail = exchange.getIn().getBody(NotificationDetail.class);

        if (detail == null) {
            detail = new NotificationDetail();
            // directly use the list from endpoint
            detail.setGroupCode(String.join(",", endpoint.getGroupCodes()));
            detail.setTemplateCode(endpoint.getTemplateCode());
            detail.setChannelCode(endpoint.getChannelCode());
        }

        List<String> groups = detail.getGroupCode() != null
                ? List.of(detail.getGroupCode().split(","))
                : endpoint.getGroupCodes();

        notificationService.sendToGroup(groups, detail.getTemplateCode(), detail.getChannelCode());
    }
}