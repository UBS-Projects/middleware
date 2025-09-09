package com.middleware.backend.users.service;

import com.middleware.backend.scheduledJobs.model.ScheduledJobs;
import com.middleware.backend.users.Roles.dto.RoleRequest;
import com.middleware.backend.users.Roles.mapper.RoleMapper;
import com.middleware.backend.users.Roles.model.Permission;
import com.middleware.backend.users.config.JwtUtil;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class UserService {
    private final UserRepository repo;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final UserMapper userMapper;

    public ResponseEntity<?> getUserById(Long id) {
        Optional<User> user = repo.findById(id);
        return user.isPresent()? ResponseEntity.ok(userMapper.mapToDto(user.get())):
                ResponseEntity.badRequest().body("Not Found");
//        List<String> roles = new ArrayList<>();
//        roles.add("ADMIN");
//        roles.add("SYSTEM_USER");
//        List<String> pers = new ArrayList<>();
//        pers.add("audit:create");
//        pers.add("user:show");
//        String token = jwtUtil.generateToken("admin@mail.com",roles,pers, 2L * 7 * 24 * 60 * 60 * 1000);
//        System.out.println("**************************************************");
//        System.out.println("System User Token: " + token);
//
//        return null;
    }

    public Page<?> getAll(Specification<User> spec, Pageable pageable) {
        Page<User> page = repo.findAll(spec, pageable);
        Page<UserResponse> res = page.map(user ->
                UserResponse.builder()
                        .id(user.getId())
                        .userName(user.getUserName())
                        .email(user.getEmail())
                        .status(user.getStatus())
                        .createdAt(user.getCreatedAt())
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

    public ResponseEntity<?> createNewUser(UserRequest user) {
        Optional<User> exists = repo.findByEmail(user.getEmail().toLowerCase());
        if(exists.isPresent())return ResponseEntity
                .badRequest()
                .body(Collections.singletonMap("message", "User Already Exists"));
        User req = userMapper.mapToEntity(user);
        req.setPassword(passwordEncoder.encode(req.getPassword()));
        req.setCreatedAt(new Timestamp(System.currentTimeMillis()));
        req.setUpdatedAt(new Timestamp(System.currentTimeMillis()));
        req.setEmail(req.getEmail().toLowerCase());
        req = repo.save(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(req);
    }

    public ResponseEntity<?> deleteUser(Long id) {
        Optional<User> user = repo.findById(id);
        if(user.isEmpty())return ResponseEntity.badRequest().body("User was not Found");
        user.get().setStatus(Status.INACTIVE);
        user.get().setUpdatedAt(new Timestamp(System.currentTimeMillis()));
        repo.save(user.get());
        return new ResponseEntity<>(HttpStatus.OK);
    }

    public ResponseEntity<?> editUser(Long id, UserRequest user) {
        Optional<User> exists = repo.findById(id);
        if(exists.isEmpty())return ResponseEntity.badRequest().body("User wasn't FOUND");
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
        repo.save(exists.get());
        return ResponseEntity.ok(exists.get());
    }

    public ResponseEntity<?> activateUser(Long id) {
        Optional<User> user = repo.findById(id);
        if(user.isEmpty())return ResponseEntity.badRequest().body("User was not Found");
        user.get().setStatus(Status.ACTIVE);
        user.get().setUpdatedAt(new Timestamp(System.currentTimeMillis()));
        repo.save(user.get());
        return new ResponseEntity<>(HttpStatus.OK);
    }

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
