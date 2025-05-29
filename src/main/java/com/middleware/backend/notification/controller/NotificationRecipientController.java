package com.middleware.backend.notification.controller;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.middleware.backend.notification.dto.NotificationRecipientDto;
import com.middleware.backend.notification.mapper.NotificationRecipientMapper;
import com.middleware.backend.notification.model.NotificationRecipient;
import com.middleware.backend.notification.repository.NotificationRecipientRepository;

import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/recipients")
@RequiredArgsConstructor
public class NotificationRecipientController {

    private final NotificationRecipientRepository repository;
    private final NotificationRecipientMapper mapper;

    @GetMapping
    public ResponseEntity<?> getAll(@RequestParam Map<String, String> filters,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id,desc") String sort) {

        Specification<NotificationRecipient> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            filters.forEach((key, value) -> {
                switch (key) {
                case "groupId" -> predicates.add(cb.equal(root.get("groupId"), Long.parseLong(value)));
                case "contactType", "contactValue", "name" -> predicates
                        .add(cb.like(cb.lower(root.get(key)), "%" + value.toLowerCase() + "%"));
                case "active" -> predicates.add(cb.equal(root.get("active"), Boolean.parseBoolean(value)));
                case "createdBy", "updatedBy" -> predicates.add(cb.equal(root.get(key), Long.parseLong(value)));
                }
            });
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Pageable pageable = PageRequest.of(page, size, Sort.by(parseSort(sort)));
        Page<NotificationRecipientDto> result = repository.findAll(spec, pageable).map(mapper::toDto);

        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<NotificationRecipientDto> getById(@PathVariable Long id) {
        return repository.findById(id)
                .map(NotificationRecipient -> ResponseEntity.ok(mapper.toDto(NotificationRecipient)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<NotificationRecipientDto> create(@RequestBody NotificationRecipientDto dto) {
        NotificationRecipient entity = mapper.toEntity(dto);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        return ResponseEntity.ok(mapper.toDto(repository.save(entity)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<NotificationRecipientDto> update(@PathVariable Long id,
            @RequestBody NotificationRecipientDto dto) {
        NotificationRecipient existing = repository.findById(id).orElseThrow();
        NotificationRecipient updated = mapper.toEntity(dto);
        updated.setId(id);
        updated.setCreatedAt(existing.getCreatedAt());
        updated.setUpdatedAt(LocalDateTime.now());
        return ResponseEntity.ok(mapper.toDto(repository.save(updated)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        repository.deleteById(id);
        return ResponseEntity.ok().build();
    }

    private List<Sort.Order> parseSort(String sort) {
        return Arrays.stream(sort.split(";")).map(item -> {
            String[] parts = item.split(",");
            return new Sort.Order(parts.length > 1 ? Sort.Direction.fromString(parts[1]) : Sort.Direction.ASC,
                    parts[0]);
        }).collect(Collectors.toList());
    }
}
