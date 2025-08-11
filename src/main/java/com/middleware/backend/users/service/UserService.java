package com.middleware.backend.users.service;

import com.middleware.backend.users.Roles.mapper.RoleMapper;
import com.middleware.backend.users.config.JwtUtil;
import com.middleware.backend.users.dto.UserRequest;
import com.middleware.backend.users.dto.UserResponse;
import com.middleware.backend.users.mapper.UserMapper;
import com.middleware.backend.users.model.Status;
import com.middleware.backend.users.model.User;
import com.middleware.backend.users.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
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

//        String token = jwtUtil.generateToken("tessdfdsdfs2332t@mail.com",5*60*1000);
//        System.out.println("**************************************************");
//        System.out.println("System User Token: " + token);
//        return null;
    }

    public ResponseEntity<?> getAll(Specification<User> spec, Pageable pageable) {
        Page<User> page = repo.findAll(spec, pageable);
        Page<UserResponse> res = page.map(user ->
                UserResponse.builder()
                        .id(user.getId())
                        .userName(user.getUserName())
                        .email(user.getEmail())
                        .status(user.getStatus())
                        .createdAt(user.getCreatedAt())
                        .password(user.getPassword())
                        .build()
        );
        if (page.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(page);
    }

    public ResponseEntity<?> createNewUser(UserRequest user) {
        Optional<User> exists = repo.findByEmail(user.getEmail());
        if(exists.isPresent())return ResponseEntity.badRequest().body("User Already Exists");
        User req = userMapper.mapToEntity(user);
        req.setPassword(passwordEncoder.encode(req.getPassword()));
        req.setCreatedAt(new Timestamp(System.currentTimeMillis()));
        req.setUpdatedAt(new Timestamp(System.currentTimeMillis()));

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
                user.getEmail() : exists.get().getEmail());

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
}
