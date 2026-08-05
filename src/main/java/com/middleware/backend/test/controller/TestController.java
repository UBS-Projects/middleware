package com.middleware.backend.test.controller;

import com.middleware.backend.test.servie.TestService;
import lombok.AllArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/test")
@AllArgsConstructor
public class TestController {
    private final TestService testService;

    @GetMapping()
    @PreAuthorize("hasAuthority('test:read')")
    public String test(@RequestParam(defaultValue = "mohammad kadoumi", name = "name") String name ) {
        return testService.greetUser(name);
    }
}
