package com.middleware.backend.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class GroupReceiversDto2 {
    private long groupId;
    private String groupName;
    private String receiverName;
}
