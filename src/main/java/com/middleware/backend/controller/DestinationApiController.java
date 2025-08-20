package com.middleware.backend.controller;

import com.middleware.backend.model.DestinationApi;
import com.middleware.backend.repository.DestinationApiRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/destination-apis")
@RequiredArgsConstructor
public class DestinationApiController {

    private final DestinationApiRepository repository;

    @PostMapping
    @PreAuthorize("hasAuthority('destinationApis:create')")
    public DestinationApi create(@RequestBody DestinationApi destinationApi) {
        return repository.save(destinationApi);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('destinationApis:view')")
    public DestinationApi getById(@PathVariable Long id) {
        return repository.findById(id).orElseThrow(() -> new RuntimeException("DestinationApi not found"));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('destinationApis:view')")
    public List<DestinationApi> getAll() {
        return repository.findAll();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('destinationApis:edit')")
    public DestinationApi update(@PathVariable Long id, @RequestBody DestinationApi updated) {
        DestinationApi existing = repository.findById(id).orElseThrow(() -> new RuntimeException("DestinationApi not found"));
        updated.setId(existing.getId());
        return repository.save(updated);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('destinationApis:delete')")
    public void delete(@PathVariable Long id) {
        repository.deleteById(id);
    }
}
