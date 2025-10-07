package com.middleware.backend.dashboard.dto;

import com.ethlo.time.DateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TransactionListDto {
    private String id;
    private String endPoint;
    private Integer responseCode;
    private LocalDateTime eventTime;
    private long duration;
}
