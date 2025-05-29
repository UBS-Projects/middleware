package com.middleware.backend.controller;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.middleware.backend.dto.ErrorMappingDto;
import com.middleware.backend.mapper.ErrorMappingMapper;
import com.middleware.backend.model.ErrorMapping;
import com.middleware.backend.repository.ErrorMappingRepository;
import com.middleware.backend.spec.ErrorMappingSpecification;

import jakarta.persistence.EntityNotFoundException;

@RestController
@RequestMapping("/api/error-mappings")
public class ErrorMappingController {

    private final ErrorMappingRepository repository;
    private final ErrorMappingMapper mapper;

    public ErrorMappingController(ErrorMappingRepository repository, ErrorMappingMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @GetMapping
    public ResponseEntity<List<ErrorMappingDto>> getAll(@RequestParam Map<String, String> filters) {
        var spec = ErrorMappingSpecification.filter(filters);
        var dtos = repository.findAll(spec)
                .stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ErrorMappingDto> getById(@PathVariable Long id) {
        return repository.findById(id)
                .map(mapper::toDto)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new EntityNotFoundException("ErrorMapping not found"));
    }

    @PostMapping
    public ResponseEntity<ErrorMappingDto> create(@RequestBody ErrorMappingDto dto) {
        var entity = mapper.toEntity(dto);
        var saved = repository.save(entity);
        return ResponseEntity.ok(mapper.toDto(saved));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ErrorMappingDto> update(@PathVariable Long id, @RequestBody ErrorMappingDto dto) {
        if (!repository.existsById(id)) {
            throw new EntityNotFoundException("ErrorMapping not found");
        }
        var entity = mapper.toEntity(dto);
        entity.setId(id);
        return ResponseEntity.ok(mapper.toDto(repository.save(entity)));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ErrorMappingDto> patch(@PathVariable Long id, @RequestBody Map<String, Object> updates) {
        var entity = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("ErrorMapping not found"));

        updates.forEach((key, value) -> {
            try {
                Field field = ErrorMapping.class.getDeclaredField(key);
                field.setAccessible(true);
                field.set(entity, value);
            } catch (Exception ignored) {
            }
        });

        return ResponseEntity.ok(mapper.toDto(repository.save(entity)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!repository.existsById(id)) {
            throw new EntityNotFoundException("ErrorMapping not found");
        }
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
