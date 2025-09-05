package com.middleware.backend.users.Roles.repository;

import com.middleware.backend.users.Roles.model.Role;
import com.middleware.backend.users.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role,Long> {
    Optional<Role> findByRoleName(String name);

    Page<Role> findAll(Specification<Role> spec, Pageable pageable);
}
