package com.middleware.backend.notification.controller;

import com.middleware.backend.notification.dto.ReceiverDto;
import com.middleware.backend.notification.model.NotificationTemplate;
import com.middleware.backend.notification.model.Receiver;
import com.middleware.backend.notification.service.ReceiverService;
import com.middleware.backend.notification.specification.NotificationTemplateSpecification;
import com.middleware.backend.notification.specification.ReceiverSpecification;
import com.middleware.backend.scheduledJobs.enums.MatchMode;
import com.middleware.backend.scheduledJobs.model.ScheduledJobs;
import com.middleware.backend.scheduledJobs.specification.ScheduledJobsSpecification;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/receivers")
@AllArgsConstructor
public class ReceiverController {
    private final ReceiverService service;

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @GetMapping("")
    public ResponseEntity<?> getAll(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String number,
            @RequestParam(required = false) String email,
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

        Specification<Receiver> spec = Specification
                .where(ReceiverSpecification.hasField("name", name, ReceiverSpecification.MatchMode.CONTAINS))
                .and(ReceiverSpecification.hasField("phone", number, ReceiverSpecification.MatchMode.CONTAINS))
                .and(ReceiverSpecification.hasField("email", email, ReceiverSpecification.MatchMode.CONTAINS))
                .and(ReceiverSpecification.dateAfter("createdAt", createdAfter))
                .and(ReceiverSpecification.dateBefore("createdAt", createdBefore));

        return ResponseEntity.ok(service.findAll(spec,pageable));
    }

    @PostMapping("")
    public ResponseEntity<?> create(@RequestBody ReceiverDto dto) {
        return ResponseEntity.ok(service.create(dto));
    }

    @PutMapping("")
    public ResponseEntity<?> update(@RequestBody ReceiverDto dto) {
        return ResponseEntity.ok(service.update(dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
