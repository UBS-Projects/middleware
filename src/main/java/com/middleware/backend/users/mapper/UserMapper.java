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

@Component
public class UserMapper {

    private final RoleRepository roleRepository;

    public UserMapper(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    public User mapToEntity(UserResponse user) {
        return User.builder()
                .id(user.getId())
                .userName(user.getUserName())
                .email(user.getEmail())
                .status(user.getStatus())
                .createdAt(user.getCreatedAt())
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

    public UserResponse mapToDto(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .userName(user.getUserName())
                .email(user.getEmail())
                .status(user.getStatus())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .password(user.getPassword())
                .roles(user.getRoles().stream()
                        .map(r -> new RoleRequest(r.getId(), r.getRoleName(),String.valueOf(r.getRoleType())))
                        .collect(Collectors.toList()))
                .build();
    }

    public User mapToEntity(UserRequest user) {
        return User.builder()
                .userName(user.getUserName())
                .email(user.getEmail())
                .status(user.getStatus())
                .createdAt(user.getCreatedAt())
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

    public UserRequest mapToDTO(User user) {
        return UserRequest.builder()
                .userName(user.getUserName())
                .email(user.getEmail())
                .status(user.getStatus())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .password(user.getPassword())
                .roles(user.getRoles().stream()
                        .map(r -> new RoleRequest(r.getId(), r.getRoleName(),String.valueOf(r.getRoleType())))
                        .collect(Collectors.toList()))
                .build();
    }
}
