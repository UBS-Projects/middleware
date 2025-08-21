package com.middleware.backend.users.controller;

import com.middleware.backend.users.dto.UserRequest;
import com.middleware.backend.users.model.MatchMode;
import com.middleware.backend.users.model.User;
import com.middleware.backend.users.service.UserService;
import com.middleware.backend.users.specification.UserSpecification;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.sql.Date;
import java.time.LocalDate;


@RestController
@RequestMapping("/user")
@AllArgsConstructor
public class UserController {
    private final UserService service;

    @GetMapping("")
    @PreAuthorize("hasAuthority('user:view')")
    public ResponseEntity<?> getAllUsers(
            @RequestParam(required = false) Long id,
            @RequestParam(required = false) String userName,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdAfter,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdBefore,
            @RequestParam(required = false, defaultValue = "updatedAt") String sortedBy,
            @RequestParam(defaultValue = "desc") String sortDirection,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, sortDirection.equalsIgnoreCase("asc")
                ? Sort.by(sortedBy).ascending()
                : Sort.by(sortedBy).descending());
        Specification<User> spec = Specification
                .where(UserSpecification.hasField("id", id, MatchMode.EXACT))
                .and(UserSpecification.hasField("userName", userName, MatchMode.CONTAINS))
                .and(UserSpecification.hasField("email", email, MatchMode.CONTAINS))
                .and(UserSpecification.hasField("status", status, MatchMode.EXACT))
                .and(UserSpecification.dateAfter("createdAt", createdAfter))
                .and(UserSpecification.dateBefore("createdAt", createdBefore));
        return service.getAll(spec,pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('user:viewById')")
    public ResponseEntity<?> getUserById(@PathVariable Long id){
        return service.getUserById(id);
    }


    @PostMapping("")
    @PreAuthorize("hasAuthority('user:create')")
    public ResponseEntity<?> createNewUser(@RequestBody UserRequest user){
        return service.createNewUser(user);
    }

    @PutMapping("/{id}/{status}")
    @PreAuthorize("hasAnyAuthority('user:edit','user:activate','user:delete')")
    public ResponseEntity<?> deleteUser(@PathVariable Long id,
                                        @PathVariable String status){
        if(status.equals("DELETE"))
            return service.deleteUser(id);
        return service.activateUser(id);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('user:edit')")
    public ResponseEntity<?> EditUser(@PathVariable("id") Long id, @RequestBody UserRequest user){
        return service.editUser(id, user);
    }



    @GetMapping("/role/{role}")
    @PreAuthorize("hasAuthority('user:viewByRole')")
    public ResponseEntity<?> getUsersByRole(@PathVariable String role,
    @RequestParam(defaultValue = "0") int page

    ){
        return service.getUsersByRole(role,page);
    }

}

