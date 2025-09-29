package com.middleware;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.middleware.model.NotificationDetail;
import com.middleware.service.NotificationServiceBridge;
import org.apache.camel.Exchange;
import org.apache.camel.support.DefaultProducer;
import java.util.List;

public class NotificationProducer extends DefaultProducer {

    private final NotificationEndpoint endpoint;
    private final NotificationServiceBridge notificationService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public NotificationProducer(NotificationEndpoint endpoint, NotificationServiceBridge notificationService) {
        super(endpoint);
        this.endpoint = endpoint;
        this.notificationService = notificationService;
    }

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
