package com.middleware.backend.test.servie;

import org.springframework.stereotype.Service;

@Service
public class TestService {
    public String greetUser(String name) {
        return "Hi "+name;
    }
}
