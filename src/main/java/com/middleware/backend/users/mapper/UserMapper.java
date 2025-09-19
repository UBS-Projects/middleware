package com.middleware.backend.users.mapper;

import com.middleware.backend.users.Roles.dto.RoleRequest;
import com.middleware.backend.users.Roles.model.Role;
import com.middleware.backend.users.Roles.repository.RoleRepository;
import com.middleware.backend.users.dto.UserRequest;
import com.middleware.backend.users.dto.UserResponse;
import com.middleware.backend.users.model.User;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Maps between user domain objects and DTOs.
 * <p>
 * When converting role DTOs to entities, role references are resolved via the
 * {@link RoleRepository} to ensure only existing roles are associated.
 */
@Component
public class UserMapper {

    private final RoleRepository roleRepository;

    public UserMapper(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    /**
     * Maps a {@link UserResponse} DTO to a {@link User} entity.
     * Role names are looked up through {@link RoleRepository}.
     * @param user response DTO
     * @return populated entity
     */
    public User mapToEntity(UserResponse user) {
        return User.builder()
                .id(user.getId())
                .userName(user.getUserName())
                .email(user.getEmail())
                .status(user.getStatus())
                .createdBy(user.getCreatedBy())
                .createdAt(user.getCreatedAt())
                .updatedBy(user.getUpdatedBy())
                .updatedAt(user.getUpdatedAt())
                .password(user.getPassword())
                .roles(user.getRoles() == null
                        ? Collections.emptyList()
                        : user.getRoles().stream()
                        .map(r -> roleRepository.findByRoleName(r.getRoleName()))
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .collect(Collectors.toList()))
                .build();
    }

    /**
     * Maps a {@link User} entity to a {@link UserResponse} DTO.
     * @param user entity
     * @return response DTO including role basic info
     */
    public UserResponse mapToDto(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .userName(user.getUserName())
                .email(user.getEmail())
                .status(user.getStatus())
                .createdBy(user.getCreatedBy())
                .createdAt(user.getCreatedAt())
                .updatedBy(user.getUpdatedBy())
                .updatedAt(user.getUpdatedAt())
                .password(user.getPassword())
                .roles(user.getRoles().stream()
                        .map(r -> new RoleRequest(r.getId(), r.getRoleName(),String.valueOf(r.getRoleType())))
                        .collect(Collectors.toList()))
                .build();
    }

    /**
     * Maps a {@link UserRequest} DTO to a {@link User} entity.
     * Role names are resolved via {@link RoleRepository}.
     * @param user request DTO
     * @return populated entity
     */
    public User mapToEntity(UserRequest user) {
        return User.builder()
                .userName(user.getUserName())
                .email(user.getEmail())
                .status(user.getStatus())
                .createdBy(user.getCreatedBy())
                .createdAt(user.getCreatedAt())
                .updatedBy(user.getUpdatedBy())
                .updatedAt(user.getUpdatedAt())
                .password(user.getPassword())
                .roles(user.getRoles() == null
                        ? Collections.emptyList()
                        : user.getRoles().stream()
                        .map(r -> roleRepository.findByRoleName(r.getRoleName()))
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .collect(Collectors.toList()))
                .build();
    }

    /**
     * Maps a {@link User} entity to a {@link UserRequest} DTO.
     * Useful for edit forms where the request structure is reused.
     * @param user entity
     * @return request DTO
     */
    public UserRequest mapToDTO(User user) {
        return UserRequest.builder()
                .userName(user.getUserName())
                .email(user.getEmail())
                .status(user.getStatus())
                .createdBy(user.getCreatedBy())
                .createdAt(user.getCreatedAt())
                .updatedBy(user.getUpdatedBy())
                .updatedAt(user.getUpdatedAt())
                .password(user.getPassword())
                .roles(user.getRoles().stream()
                        .map(r -> new RoleRequest(r.getId(), r.getRoleName(),String.valueOf(r.getRoleType())))
                        .collect(Collectors.toList()))
                .build();
    }
}
