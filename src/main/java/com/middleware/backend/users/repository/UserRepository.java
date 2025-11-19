package com.middleware.backend.users.repository;

import com.middleware.backend.users.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for managing {@link User} entities.
 */
@Repository
public interface UserRepository extends JpaRepository<User,Long> {
    /**
     * Finds users by a JPA {@link Specification} with pagination.
     */
    Page<User> findAll(Specification<User> spec, Pageable pageable);

    /**
     * Finds a user by email (any status).
     */
    Optional<User> findByEmail(String email);

    /**
     * Finds an ACTIVE user by email.
     */
    @Query("SELECT u FROM User u WHERE u.email = :email AND u.status = 'ACTIVE'")
    Optional<User> findActiveByEmail(@Param("email") String email);

    /**
     * Finds an ACTIVE user by id.
     */
    @Query("SELECT u FROM User u WHERE u.id = :id AND u.status = 'ACTIVE'")
    Optional<User> findActiveById(@Param("id") Long id);

    /**
     * Pages users by role name.
     */
    Page<User> findByRoles_RoleName(String Role, Pageable pageable);

    @Query("""
        SELECT DISTINCT u
        FROM User u
        JOIN u.roles r
        WHERE r.roleName = 'ADMIN' AND u.status = 'ACTIVE'
        """)
    List<User> findAllAdmins();
}
