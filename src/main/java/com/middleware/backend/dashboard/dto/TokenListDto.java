package com.middleware.backend.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TokenListDto {
    private Long id;
    private String userName;
    private Long userId;
    private Timestamp expiryDate;
}
