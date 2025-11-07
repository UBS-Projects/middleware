package com.middleware.backend.users.tokens.service;

import com.middleware.backend.users.tokens.model.Token;
import com.middleware.backend.users.tokens.repository.TokenRepository;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Service class for handling token-related operations.
 * <p>
 * Provides methods for saving, removing, and invalidating tokens.
 * </p>
 */
@Service
@AllArgsConstructor
public class TokenService {

    /** Repository for token persistence */
    private final TokenRepository repo;

    /**
     * Saves a token for a user.
     * <p>
     * If tokens already exist for the user, they are removed before saving the new one.
     * </p>
     *
     * @param token the token entity to save
     */
    public void save(Token token) {
        List<Token> existing = repo.findAllByUser_Id(token.getUser().getId()).orElse(List.of());

        if (!existing.isEmpty()) {
            Token oldToken = existing.get(0);
            oldToken.setToken(token.getToken());
            oldToken.setCreatedAt(token.getCreatedAt());
            oldToken.setExpiresAt(token.getExpiresAt());
            oldToken.setValid(true);
            repo.save(oldToken); // update same row → same id
        } else {
            repo.save(token); // first time → new id
        }
    }

    /**
     * Logs out a user by invalidating their token.
     *
     * @param email the user’s email
     * @return a {@link ResponseEntity} containing the updated token
     * @throws java.util.NoSuchElementException if no token is found for the email
     */
    public ResponseEntity<?> logout(String email) {
        Optional<Token> tok = repo.findByUser_Email(email);
        tok.get().setValid(false);
        return ResponseEntity.ok(repo.save(tok.get()));
    }
}
