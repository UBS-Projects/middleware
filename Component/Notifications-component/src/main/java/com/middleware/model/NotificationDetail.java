package com.middleware.model;

import lombok.Getter;
import lombok.Setter;

/**
 * A data transfer object for notification details.
 */
@Getter
@Setter
public class NotificationDetail {
    private String groupCode;
    private String templateCode;
    private String channelCode;
}
