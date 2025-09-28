package com.middleware.backend.notification.controller;

import com.middleware.backend.notification.dto.NotificationTemplateDto;
import com.middleware.backend.notification.enums.ChannelType;
import com.middleware.backend.notification.model.NotificationTemplate;
import com.middleware.backend.notification.service.NotificationTemplateService;
import com.middleware.backend.notification.specification.NotificationTemplateSpecification;
import com.middleware.backend.system_settings.model.Config;
import com.middleware.backend.system_settings.model.ConfigType;
import com.middleware.backend.system_settings.specificaion.ConfigSpecification;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.time.LocalDate;
import java.util.Map;

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
            @RequestParam(required = false) String templateName,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String type, // string from request
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdAfter,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdBefore,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false, defaultValue = "updatedAt") String sortedBy,
            @RequestParam(defaultValue = "desc") String sortDirection
    ) {
        Pageable pageable = PageRequest.of(page, size, sortDirection.equalsIgnoreCase("asc")
                ? Sort.by(sortedBy).ascending()
                : Sort.by(sortedBy).descending());




        ChannelType channelType = null;
        if (type != null) {
            try {
                channelType = ChannelType.valueOf(type.toUpperCase());
            } catch (IllegalArgumentException e) {
                // invalid type string, ignore
            }
        }

        Specification<NotificationTemplate> spec = Specification
                .where(NotificationTemplateSpecification.hasField("name", templateName, NotificationTemplateSpecification.MatchMode.CONTAINS))
                .and(NotificationTemplateSpecification.hasField("code", code, NotificationTemplateSpecification.MatchMode.CONTAINS))
                .and(NotificationTemplateSpecification.hasType(channelType))
                .and(NotificationTemplateSpecification.dateAfter("createdAt", createdAfter))
                .and(NotificationTemplateSpecification.dateBefore("createdAt", createdBefore));



        return ResponseEntity.ok(service.findAll(spec, pageable));
    }

    @PostMapping("")
    public ResponseEntity<?> create(@Valid @RequestBody NotificationTemplateDto dto) {
        return ResponseEntity.ok(service.create(dto));
    }

    @PutMapping("")
    public ResponseEntity<?> update(@Valid @RequestBody NotificationTemplateDto dto) {
        return ResponseEntity.ok(service.update(dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

     @PostMapping("/validate-json")
    public ResponseEntity<?> validateJsonStructure(@RequestBody String jsonString) {
        return ResponseEntity.ok(service.validateJsonStructure(jsonString));
    }

    @GetMapping("/all")
    public ResponseEntity<?> getAllReceivers(
            @RequestParam(required = false) String search
    ) {
        return ResponseEntity.ok(service.getReceivers(search));
    }
}