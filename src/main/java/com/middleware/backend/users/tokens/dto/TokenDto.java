package com.middleware.backend.users.tokens.dto;

import com.middleware.backend.users.model.User;
import jakarta.persistence.Column;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TokenDto {
    private Long id;
    private Long userId;
    private String token;
    private boolean isValid;
    private Timestamp createdAt;
    private Timestamp expiresAt;
}
