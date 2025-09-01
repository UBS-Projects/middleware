package com.middleware.backend.users.tokens.mapper;

import com.middleware.backend.users.repository.UserRepository;
import com.middleware.backend.users.tokens.dto.TokenDto;
import com.middleware.backend.users.tokens.model.Token;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class TokenMapper {
    private static UserRepository userRepo;
    public static Token mapToEntity(TokenDto token){
        return Token.builder()
                .id(token.getId())
                .user(userRepo.findById(token.getUserId()).get())
                .token(token.getToken())
                .isValid(token.isValid())
                .createdAt(token.getCreatedAt())
                .expiresAt(token.getExpiresAt())
                .build();
    }

    public static TokenDto mapToDto(Token token){
        return TokenDto.builder()
                .id(token.getId())
                .userId(token.getUser().getId())
                .token(token.getToken())
                .isValid(token.isValid())
                .createdAt(token.getCreatedAt())
                .expiresAt(token.getExpiresAt())
                .build();
    }
}
