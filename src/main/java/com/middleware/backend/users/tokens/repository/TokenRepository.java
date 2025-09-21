package com.middleware.backend.users.tokens.repository;

import com.middleware.backend.users.tokens.model.Token;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for performing CRUD operations on {@link Token} entities.
 * <p>
 * Extends {@link JpaRepository} to provide built-in methods and defines
 * custom queries for token-related operations.
 * </p>
 */
@Repository
public interface TokenRepository extends JpaRepository<Token, Long> {

    /**
     * Finds all tokens associated with a given user ID.
     *
     * @param id the user ID
     * @return an optional containing a list of tokens if present
     */
    Optional<List<Token>> findAllByUser_Id(Long id);

    /**
     * Deletes all tokens belonging to a specific user.
     *
     * @param userId the user ID
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM Token t WHERE t.user.id = :userId")
    void removeByUser_Id(Long userId);

    /**
     * Checks if a valid token exists for a given email and token string.
     *
     * @param email the user email
     * @param token the token string
     * @return true if a valid token exists, false otherwise
     */
    boolean existsByUser_EmailAndTokenAndIsValidTrue(String email, String token);

    /**
     * Finds a token by its string value.
     *
     * @param token the token string
     * @return an optional containing the token if found
     */
    Optional<Token> findByToken(String token);

    /**
     * Finds a token associated with a user’s email.
     *
     * @param email the user email
     * @return an optional containing the token if found
     */
    Optional<Token> findByUser_Email(String email);
}
