package com.middleware.backend.integrated_systems.controller;

import com.middleware.backend.integrated_systems.dto.IntegratedSystemDto;
import com.middleware.backend.integrated_systems.model.IntegratedSystem;
import com.middleware.backend.integrated_systems.service.IntegratedSystemService;
import com.middleware.backend.integrated_systems.spec.IntegratedSystemSpecification;
import com.middleware.backend.users.specification.UserSpecification;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/integrated-system")
@AllArgsConstructor
public class IntegratedSystemController {

    private final IntegratedSystemService service;


    @GetMapping("")
    public ResponseEntity<Page<?>> getAll(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String host,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String protocol,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdAfter,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdBefore,
            @RequestParam(required = false, defaultValue = "updatedAt") String sortedBy,
            @RequestParam(defaultValue = "desc") String sortDirection,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size

    ){
        Pageable pageable = PageRequest.of(page, size, sortDirection.equalsIgnoreCase("asc")
                ? Sort.by(sortedBy).ascending()
                : Sort.by(sortedBy).descending());

        Specification<IntegratedSystem> spec = Specification
                .where(IntegratedSystemSpecification.hasField("code", code, IntegratedSystemSpecification.MatchMode.CONTAINS))
                .and(IntegratedSystemSpecification.hasField("host", host, IntegratedSystemSpecification.MatchMode.CONTAINS))
                .and(IntegratedSystemSpecification.hasField("description", description, IntegratedSystemSpecification.MatchMode.CONTAINS))
                .and(IntegratedSystemSpecification.hasField("protocol", protocol, IntegratedSystemSpecification.MatchMode.CONTAINS))
                .and(IntegratedSystemSpecification.dateAfter("createdAt", createdAfter))
                .and(IntegratedSystemSpecification.dateBefore("createdAt", createdBefore));

        return ResponseEntity.ok(service.getAll(spec,pageable));
    }

    @GetMapping("/{code}")
    public ResponseEntity<?> getById(@PathVariable String code){
        return ResponseEntity.ok(service.getById(code));
    }

    @PostMapping()
    public ResponseEntity<?> IntegrateNewSystem(@RequestBody IntegratedSystemDto body){
        IntegratedSystemDto response = service.create(body);
        if (response == null)
            return ResponseEntity.badRequest().body("Key Already Exists");
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    @PutMapping
    public ResponseEntity<?> update(@RequestBody IntegratedSystemDto body){
        IntegratedSystemDto response = service.update(body);
        if (response == null)
            return ResponseEntity.badRequest().body("Key wasn't' Found");
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
