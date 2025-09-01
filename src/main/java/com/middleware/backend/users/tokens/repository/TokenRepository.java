package com.middleware.backend.users.tokens.repository;

import com.middleware.backend.users.tokens.model.Token;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TokenRepository extends JpaRepository<Token,Long> {
    Optional<List<Token>> findAllByUser_Id(Long id);

    @Modifying
    @Transactional
    @Query("DELETE FROM Token t WHERE t.user.id = :userId")
    void removeByUser_Id(Long userId);

    boolean existsByUser_EmailAndTokenAndIsValidTrue(String email, String token);

    Optional<Token> findByToken(String token);

    Optional<Token> findByUser_Email(String email);
}
