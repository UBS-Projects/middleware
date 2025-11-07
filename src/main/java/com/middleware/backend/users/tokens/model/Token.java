package com.middleware.backend.users.tokens.model;

import com.middleware.backend.users.model.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

/**
 * Entity representing an authentication token associated with a {@link User}.
 * <p>
 * Maps to the "user_tokens" table in the database.
 * </p>
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "user_tokens")
public class Token {

    /** Primary key identifier for the token */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The user this token belongs to */
    @ManyToOne
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    private User user;

    /** The actual token string value */
    @Column(length = 10000)
    private String token;

    /** Indicates if the token is currently valid */
    @Column(name = "is_valid")
    private boolean isValid;

    /** Timestamp when the token was created */
    @Column(name = "created_at")
    private Timestamp createdAt;

    /** Timestamp when the token will expire */
    @Column(name = "expires_at")
    private Timestamp expiresAt;
}
