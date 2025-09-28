package com.middleware.backend.notification.controller;

import com.middleware.backend.notification.dto.ChannelConfigDto;
import com.middleware.backend.notification.enums.ChannelType;
import com.middleware.backend.notification.model.ChannelConfig;
import com.middleware.backend.notification.model.NotificationTemplate;
import com.middleware.backend.notification.service.ChannelService;
import com.middleware.backend.notification.specification.ChannelConfigSpecification;
import com.middleware.backend.notification.specification.NotificationTemplateSpecification;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/channels")
@AllArgsConstructor
public class ChannelController {
    private final ChannelService service;

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @GetMapping("")
    public ResponseEntity<?> getAll(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String config,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdAfter,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdBefore,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "updatedAt") String sortedBy,
            @RequestParam(defaultValue = "desc") String sortDirection
    ) {
        Pageable pageable = PageRequest.of(page, size,
                sortDirection.equalsIgnoreCase("asc") ? Sort.by(sortedBy).ascending() : Sort.by(sortedBy).descending());


        ChannelType channelType = null;
        if (type != null) {
            try {
                channelType = ChannelType.valueOf(type.toUpperCase());
            } catch (IllegalArgumentException e) {
                // invalid type string, ignore
            }
        }

        Specification<ChannelConfig> spec = Specification
                .where(ChannelConfigSpecification.hasField("name", name, ChannelConfigSpecification.MatchMode.CONTAINS))
                .and(ChannelConfigSpecification.hasField("code", code, ChannelConfigSpecification.MatchMode.CONTAINS))
                .and(ChannelConfigSpecification.hasField("config", config, ChannelConfigSpecification.MatchMode.CONTAINS))
                .and(ChannelConfigSpecification.hasType(channelType))
                .and(ChannelConfigSpecification.dateAfter("createdAt", createdAfter))
                .and(ChannelConfigSpecification.dateBefore("createdAt", createdBefore));

        return ResponseEntity.ok(service.findAll(spec, pageable));
    }

    @PostMapping("")
    public ResponseEntity<?> create(@RequestBody ChannelConfigDto dto) {
        return ResponseEntity.ok(service.create(dto));
    }

    @PutMapping("")
    public ResponseEntity<?> update(@RequestBody ChannelConfigDto dto) {
        return ResponseEntity.ok(service.update(dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
    @GetMapping("/all")
    public ResponseEntity<?> getAllReceivers(
            @RequestParam(required = false) String search
    ) {
        return ResponseEntity.ok(service.getReceivers(search));
    }
}
