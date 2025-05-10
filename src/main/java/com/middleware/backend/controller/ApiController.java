package com.middleware.backend.controller;

import com.middleware.backend.model.ApiEndpoint;
import com.middleware.backend.service.ApiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

//@RestController
//@RequestMapping("/api/endpoints")
public class ApiController {

    @Autowired
    private ApiService apiService;

    @GetMapping
    public List<ApiEndpoint> getAll() {
        return apiService.getAllEndpoints();
    }

    @PostMapping
    public ApiEndpoint create(@RequestBody ApiEndpoint apiEndpoint) {
        return apiService.save(apiEndpoint);
    }
}
