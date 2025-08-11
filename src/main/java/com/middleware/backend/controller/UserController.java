//package com.middleware.backend.controller;
//
//import com.middleware.backend.model.User;
//import com.middleware.backend.service.AuthService;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.Optional;
//
////@RestController
////@RequestMapping("/api/public")
//public class UserController {
//
//    @Autowired
//    private AuthService authService;
//
//    @PostMapping("/register")
//    public User register(@RequestParam String username, @RequestParam String password) {
//        return authService.register(username, password);
//    }
//
//    @PostMapping("/login")
//    public String login(@RequestParam String username, @RequestParam String password) {
//        Optional<User> user = authService.authenticate(username, password);
//        return user.isPresent() ? "Login successful!" : "Invalid credentials";
//    }
//}
