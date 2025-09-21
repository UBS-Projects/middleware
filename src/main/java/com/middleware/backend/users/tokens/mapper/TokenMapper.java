package com.middleware.backend.users.tokens.mapper;

import com.middleware.backend.users.repository.UserRepository;
import com.middleware.backend.users.tokens.dto.TokenDto;
import com.middleware.backend.users.tokens.model.Token;
import lombok.AllArgsConstructor;

/**
 * Mapper class for converting between {@link Token} entities and {@link TokenDto} objects.
 * <p>
 * Provides static utility methods to transform data for persistence or presentation.
 * </p>
 */
@AllArgsConstructor
public class TokenMapper {

    /** Repository used to fetch User entities for mapping */
    private static UserRepository userRepo;

    /**
     * Maps a {@link TokenDto} to a {@link Token} entity.
     *
     * @param token the DTO to convert
     * @return the corresponding Token entity
     * @throws java.util.NoSuchElementException if the userId is not found in the repository
     */
    public static Token mapToEntity(TokenDto token) {
        return Token.builder()
                .id(token.getId())
                .user(userRepo.findById(token.getUserId()).get())
                .token(token.getToken())
                .isValid(token.isValid())
                .createdAt(token.getCreatedAt())
                .expiresAt(token.getExpiresAt())
                .build();
    }

    /**
     * Maps a {@link Token} entity to a {@link TokenDto}.
     *
     * @param token the entity to convert
     * @return the corresponding DTO
     */
    public static TokenDto mapToDto(Token token) {
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
