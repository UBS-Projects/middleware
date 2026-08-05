package com.middleware.backend.users.tokens.repository;

import com.middleware.backend.dashboard.dto.TokenListDto;
import com.middleware.backend.users.Roles.model.Role;
import com.middleware.backend.users.tokens.model.Token;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
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





//    ****************************** Dashboard methods
@Query("""
    SELECT DISTINCT new com.middleware.backend.dashboard.dto.TokenListDto(
        t.id,
        t.user.userName,
        t.user.id,
        t.expiresAt
    )
    FROM Token t
    JOIN t.user.roles r
    WHERE ((t.expiresAt <= :now AND t.expiresAt >= :sevenDaysAgo)
       OR (t.expiresAt > :now AND t.expiresAt <= :twoWeeksLater))
      AND r.roleType = :roleType
    ORDER BY t.expiresAt ASC
""")
List<TokenListDto> findExpiringOrRecentlyExpiredTokensByRole(
        @Param("now") Timestamp now,
        @Param("sevenDaysAgo") Timestamp sevenDaysAgo,
        @Param("twoWeeksLater") Timestamp twoWeeksLater,
        @Param("roleType") Role.RoleType roleType,
        Pageable pageable
);



    // Expired strictly before now
    @Query("""
        SELECT t
        FROM Token t
        JOIN t.user u
        JOIN u.roles r
        WHERE t.expiresAt < :now
          AND r.roleType = com.middleware.backend.users.Roles.model.Role.RoleType.SYSTEM_USER
    """)
    List<Token> findExpiredSystemUserTokens(@Param("now") Timestamp now);



    // Will expire in the given window (inclusive)
    @Query("""
        SELECT t
        FROM Token t
        JOIN t.user u
        JOIN u.roles r
        WHERE t.expiresAt BETWEEN :from AND :to
          AND r.roleType = com.middleware.backend.users.Roles.model.Role.RoleType.SYSTEM_USER
    """)
    List<Token> findSystemUserTokensExpiringBetween(
            @Param("from") Timestamp from,
            @Param("to") Timestamp to
    );
}
