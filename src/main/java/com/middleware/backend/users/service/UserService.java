package com.middleware.backend.users.service;

import com.middleware.backend.keycloak.service.KeycloakUserSyncService;
import com.middleware.backend.users.Roles.dto.RoleRequest;
import com.middleware.backend.users.Roles.mapper.RoleMapper;
import com.middleware.backend.users.Roles.model.Role;
//import com.middleware.backend.users.config.JwtUtil;
import com.middleware.backend.users.dto.UserRequest;
import com.middleware.backend.users.dto.UserResponse;
import com.middleware.backend.users.dto.UserResponseRoles;
import com.middleware.backend.users.mapper.UserMapper;
import com.middleware.backend.users.model.Status;
import com.middleware.backend.users.model.User;
import com.middleware.backend.users.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Business logic for user management.
 * <p>
 * Handles CRUD operations, status changes, pagination and filtering, password
 * encoding, and mapping between entities and DTOs.
 */
@Service
@AllArgsConstructor
public class UserService {
    private final UserRepository repo;
    private final PasswordEncoder passwordEncoder;
//    private final JwtUtil jwtUtil;
    private final UserMapper userMapper;
    KeycloakUserSyncService keycloakUserSyncService;
    /**
     * Retrieves a user by id.
     * @param id user id
     * @return 200 with {@link UserResponse} or 400 if not found
     */
    public ResponseEntity<?> getUserById(Long id) {
        Optional<User> user = repo.findById(id);
        return user.isPresent()? ResponseEntity.ok(userMapper.mapToDto(user.get())):
                ResponseEntity.badRequest().body("Not Found");
    }

    /**
     * Returns a paginated list of users mapped to DTO, applying the provided specification.
     * @param spec filter specification
     * @param pageable pagination and sorting
     * @return page of {@link UserResponse}
     */
    public Page<?> getAll(Specification<User> spec, Pageable pageable) {
        Page<User> page = repo.findAll(spec, pageable);
        Page<UserResponse> res = page.map(user ->
                UserResponse.builder()
                        .id(user.getId())
                        .userName(user.getUserName())
                        .email(user.getEmail())
                        .status(user.getStatus())
                        .createdBy(user.getCreatedBy())
                        .createdAt(user.getCreatedAt())
                        .updatedBy(user.getUpdatedBy())
                        .updatedAt(user.getUpdatedAt())
                        .password(user.getPassword())
                        .roles(user.getRoles().stream().map(
                                r-> RoleRequest.builder()
                                        .roleName(r.getRoleName())
                                        .roleType(String.valueOf(r.getRoleType()))
                                        .build()
                        ).toList())
                        .build()
        );

        return res;
    }

    /**
     * Creates a new user, encoding the password and setting audit fields.
     * Rejects duplicate emails.
     * @param user request payload
     * @return 201 with created entity or 400 if exists
     */

    private boolean isRoleTypeUnified(List<RoleRequest> roles){
        String type = "";
        if(!roles.isEmpty()){
            type = roles.get(0).getRoleType();
        }
        for(RoleRequest r : roles){
            if(!r.getRoleType().equals(type)){
                return false;
            }
        }
        return true;
    }
    public ResponseEntity<?> createNewUser(UserRequest user) {

        Optional<User> exists = repo.findByEmail(user.getEmail().toLowerCase());
        if(exists.isPresent())return ResponseEntity
                .badRequest()
                .body(Collections.singletonMap("message", "User Already Exists"));


        if(!isRoleTypeUnified(user.getRoles()))
            return ResponseEntity.badRequest().body("User Must not Hold USER and SYSTEM_SERVICE ROLES");


        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String emailUser = authentication.getName();
        User req = userMapper.mapToEntity(user);
        req.setPassword(passwordEncoder.encode(req.getPassword()));
        req.setCreatedAt(new Timestamp(System.currentTimeMillis()));
        req.setUpdatedAt(new Timestamp(System.currentTimeMillis()));
        req.setCreatedBy(emailUser);
        req.setUpdatedBy(emailUser);
        req.setEmail(req.getEmail().toLowerCase());
        req = repo.save(req);
        // ⬇️ sync this new user to Keycloak
        try {
            keycloakUserSyncService.syncUserByIdToKeycloak(req.getId());
        } catch (Exception e) {
            // don't fail the API if Keycloak is down
            // (optional) log the error with your logger
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(req);
    }

    /**
     * Soft-deletes a user by setting status to INACTIVE and updating audit fields.
     * @param id user id
     * @return 200 when updated or 400 if not found
     */
    public ResponseEntity<?> deleteUser(Long id) {
        Optional<User> user = repo.findById(id);
        if(user.isEmpty())return ResponseEntity.badRequest().body("User was not Found");
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String emailUser = authentication.getName();
        user.get().setStatus(Status.INACTIVE);
        user.get().setUpdatedAt(new Timestamp(System.currentTimeMillis()));
        user.get().setUpdatedBy(emailUser);
        repo.save(user.get());
        return new ResponseEntity<>(HttpStatus.OK);
    }

    /**
     * Edits an existing user; selectively updates fields and encodes password if provided.
     * Also replaces roles when provided.
     * @param id user id
     * @param user partial update payload
     * @return 200 with updated entity or 400 if not found
     */
    public ResponseEntity<?> editUser(Long id, UserRequest user) {
        Optional<User> exists = repo.findById(id);
        if(exists.isEmpty())return ResponseEntity.badRequest().body("User wasn't FOUND");

        if(!isRoleTypeUnified(user.getRoles()))
            return ResponseEntity.badRequest().body("User Must not Hold USER and SYSTEM_SERVICE ROLES");

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String emailUser = authentication.getName();
        exists.get().setUserName(!user.getUserName().isEmpty() ?
                user.getUserName() : exists.get().getUserName());

        exists.get().setEmail(!user.getEmail().isEmpty() ?
                user.getEmail().toLowerCase() : exists.get().getEmail().toLowerCase());

        exists.get().setStatus(user.getStatus());
        exists.get().setUpdatedAt(new Timestamp(System.currentTimeMillis()));
        exists.get().setPassword(user.getPassword()==null ?
                exists.get().getPassword():passwordEncoder.encode(user.getPassword()));
        exists.get().setRoles(user.getRoles() == null ?
                exists.get().getRoles() :
                user.getRoles().stream()
                        .map(RoleMapper::mapToEntity)
                        .collect(Collectors.toList())
        );
        exists.get().setUpdatedBy(emailUser);
        repo.save(exists.get());
        return ResponseEntity.ok(exists.get());
    }

    /**
     * Activates a user by setting status to ACTIVE and updating audit fields.
     * @param id user id
     * @return 200 when updated or 400 if not found
     */
    public ResponseEntity<?> activateUser(Long id) {
        Optional<User> user = repo.findById(id);
        if(user.isEmpty())return ResponseEntity.badRequest().body("User was not Found");
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String emailUser = authentication.getName();
        user.get().setStatus(Status.ACTIVE);
        user.get().setUpdatedAt(new Timestamp(System.currentTimeMillis()));
        user.get().setUpdatedBy(emailUser);
        repo.save(user.get());
        return new ResponseEntity<>(HttpStatus.OK);
    }

    /**
     * Retrieves users by role name with pagination and returns a minimal projection.
     * @param role role name
     * @param page page index (size fixed to 10)
     * @return page of {@link UserResponseRoles}
     */
    public ResponseEntity<?> getUsersByRole(String role, int page) {
        Page<UserResponseRoles> users = repo.findByRoles_RoleName(role.toUpperCase(), PageRequest.of(page,10)).map(
                user -> UserResponseRoles.builder()
                        .id(user.getId())
                        .userName(user.getUserName())
                        .email(user.getEmail())
                        .status(user.getStatus()).build());
        return ResponseEntity.ok(users);
    }
}
