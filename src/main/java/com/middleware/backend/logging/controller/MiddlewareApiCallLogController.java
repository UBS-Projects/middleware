package com.middleware.backend.logging.controller;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.middleware.backend.logging.dto.MiddlewareApiCallLogDto;
import com.middleware.backend.logging.mapper.MiddlewareApiCallLogMapper;
import com.middleware.backend.logging.model.MiddlewareApiCallLog;
import com.middleware.backend.logging.repository.MiddlewareApiCallLogRepository;

import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/logs")
@RequiredArgsConstructor
public class MiddlewareApiCallLogController {

    private final MiddlewareApiCallLogRepository repository;
    private final MiddlewareApiCallLogMapper mapper;

    @GetMapping
    // public ResponseEntity<Map<String, Object>>
    public ResponseEntity<?> getAll(@RequestParam Map<String, String> filters,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size,
            @RequestParam(value = "sort", defaultValue = "id,desc") String sortParam) {

        Specification<MiddlewareApiCallLog> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            filters.forEach((key, value) -> {
                if (value == null || value.isBlank())
                    return;

                try {
                    switch (key) {
                    case "durationMsMin" -> predicates.add(cb.ge(root.get("durationMs"), Long.parseLong(value)));
                    case "durationMsMax" -> predicates.add(cb.le(root.get("durationMs"), Long.parseLong(value)));
                    case "retryCountMin" -> predicates.add(cb.ge(root.get("retryCount"), Integer.parseInt(value)));
                    case "retryCountMax" -> predicates.add(cb.le(root.get("retryCount"), Integer.parseInt(value)));

                    case "responseCode" -> predicates.add(cb.equal(root.get(key), Integer.parseInt(value)));
                    case "throttlingApplied" -> predicates.add(cb.equal(root.get(key), Boolean.parseBoolean(value)));

                    case "receivedAtFrom" -> predicates
                            .add(cb.greaterThanOrEqualTo(root.get("receivedAt"), LocalDateTime.parse(value)));
                    case "receivedAtTo" -> predicates
                            .add(cb.lessThanOrEqualTo(root.get("receivedAt"), LocalDateTime.parse(value)));

                    case "completedAtFrom" -> predicates
                            .add(cb.greaterThanOrEqualTo(root.get("completedAt"), LocalDateTime.parse(value)));
                    case "completedAtTo" -> predicates
                            .add(cb.lessThanOrEqualTo(root.get("completedAt"), LocalDateTime.parse(value)));

                    case "id", "apiEndpointId", "workflowId", "apiKeyId", "userId" -> predicates
                            .add(cb.equal(root.get(key), Long.parseLong(value)));

                    case "responseCodeGt" -> predicates.add(cb.gt(root.get("responseCode"), Integer.parseInt(value)));
                    case "responseCodeLt" -> predicates.add(cb.lt(root.get("responseCode"), Integer.parseInt(value)));
                    case "responseCodeMin" -> predicates.add(cb.ge(root.get("responseCode"), Integer.parseInt(value)));
                    case "responseCodeMax" -> predicates.add(cb.le(root.get("responseCode"), Integer.parseInt(value)));

                    default -> predicates.add(cb.like(cb.lower(root.get(key)), "%" + value.toLowerCase() + "%"));
                    }
                } catch (Exception ignored) {
                }
            });

            return cb.and(predicates.toArray(new Predicate[0]));
        };
        /*
         * List<Sort.Order> orders = new ArrayList<>(); for (String sortItem : sort) {
         * String[] parts = sortItem.split(","); if (parts.length == 2) { orders.add(new
         * Sort.Order(Sort.Direction.fromString(parts[1].toUpperCase()), parts[0])); }
         * else { orders.add(new Sort.Order(Sort.Direction.ASC, parts[0])); } }
         */

        String[] sortFields = sortParam.split(";");

        List<Sort.Order> orders = new ArrayList<>();
        for (String sortField : sortFields) {
            String[] parts = sortField.split(",");
            if (parts.length == 2) {
                orders.add(new Sort.Order(Sort.Direction.fromString(parts[1]), parts[0]));
            } else {
                orders.add(new Sort.Order(Sort.Direction.ASC, sortField));
            }
        }
        Pageable pageable = PageRequest.of(page, size, Sort.by(orders));
        Page<MiddlewareApiCallLog> pageLogs = repository.findAll(spec, pageable);

        List<MiddlewareApiCallLogDto> dtos = pageLogs.getContent().stream().map(mapper::toDto)
                .collect(Collectors.toList());

        // Pageable pageable = PageRequest.of(page, size, Sort.by(parseSort(sort)));
        Page<MiddlewareApiCallLogDto> result = repository.findAll(spec, pageable).map(mapper::toDto);

        // List<MiddlewareApiCallLog> ent =
        // pageLogs.getContent().stream().collect(Collectors.toList());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("content", dtos);
        response.put("currentPage", pageLogs.getNumber());
        response.put("totalItems", pageLogs.getTotalElements());
        response.put("totalPages", pageLogs.getTotalPages());

        return ResponseEntity.ok(result); // or response
    }
}
