package com.middleware.backend.notification.controller;

import com.middleware.backend.notification.dto.NotificationTemplateDto;
import com.middleware.backend.notification.service.NotificationTemplateService;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/templates")
@AllArgsConstructor
public class TemplateController {
    private final NotificationTemplateService service;

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @GetMapping("")
    public ResponseEntity<?> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false, defaultValue = "updatedAt") String sortedBy,
            @RequestParam(defaultValue = "desc") String sortDirection
    ) {
        Pageable pageable = PageRequest.of(page, size, sortDirection.equalsIgnoreCase("asc")
                ? Sort.by(sortedBy).ascending()
                : Sort.by(sortedBy).descending());
        return ResponseEntity.ok(service.findAll(pageable));
    }

    @PostMapping("")
    public ResponseEntity<?> create(@RequestBody NotificationTemplateDto dto) {
        return ResponseEntity.ok(service.create(dto));
    }

    @PutMapping("")
    public ResponseEntity<?> update(@RequestBody NotificationTemplateDto dto) {
        return ResponseEntity.ok(service.update(dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
