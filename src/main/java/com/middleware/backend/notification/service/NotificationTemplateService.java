package com.middleware.backend.notification.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.middleware.backend.notification.model.NotificationActionsLogs;
import com.middleware.backend.notification.dto.NotificationTemplateDto;
import com.middleware.backend.notification.dto.ReceiverRequest;
import com.middleware.backend.notification.enums.ChannelType;
import com.middleware.backend.notification.mapper.TemplateMapper;
import com.middleware.backend.notification.model.NotificationTemplate;
import com.middleware.backend.notification.repository.TemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class NotificationTemplateService {
    private final TemplateRepository repo;
    private final NotificationActionsLogsService loggingService;

    public NotificationTemplateDto findById(Long id) {
        return repo.findById(id).map(TemplateMapper::MapToDto)
                .orElseThrow(() -> new RuntimeException("Template not found"));
    }

    public Page<NotificationTemplateDto> findAll(Specification<NotificationTemplate> spec,Pageable pageable) {
        return repo.findAll(spec,pageable).map(TemplateMapper::MapToDto);
    }

    public NotificationTemplateDto create(NotificationTemplateDto dto) {
        repo.findByName(dto.getName().trim())
                .ifPresent(t -> { throw new RuntimeException("Template Name already exists"); });

        repo.findByCode(dto.getCode().trim())
                .ifPresent(t -> { throw new RuntimeException("Template Code already exists"); });

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUser = authentication.getName();
        dto.setName(dto.getName().trim());
        dto.setCode(dto.getCode().trim());
        dto.setCreatedBy(currentUser);
        dto.setCreatedAt(new Timestamp(System.currentTimeMillis()));
        dto.setUpdatedBy(currentUser);
        dto.setUpdatedAt(new Timestamp(System.currentTimeMillis()));

        NotificationTemplate saved = repo.save(TemplateMapper.MapToEntity(dto));


        loggingService.save(NotificationActionsLogs.builder()
                        .action("POST")
                        .details("Created New Template "+dto.getName())
                        .email(currentUser)
                        .eventTime(new Timestamp(System.currentTimeMillis()))
                .build()

        );
        return TemplateMapper.MapToDto(saved);
    }

    public NotificationTemplateDto update(NotificationTemplateDto dto) {
        NotificationTemplate entity = repo.findById(dto.getId())
                .orElseThrow(() -> new RuntimeException("Template not found"));

        repo.findByName(dto.getName().trim())
                .orElseThrow(() -> new RuntimeException("Template Name Exists"));


        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUser = authentication.getName();
        entity.setBody(dto.getBody());
        entity.setSubject(dto.getSubject());
        entity.setType(ChannelType.valueOf(dto.getType()));
        entity.setUpdatedBy(currentUser);
        entity.setUpdatedAt(new Timestamp(System.currentTimeMillis()));

        loggingService.save(NotificationActionsLogs.builder()
                .action("PUT")
                .details("Updated Template "+dto.getName())
                .email(currentUser)
                .eventTime(new Timestamp(System.currentTimeMillis()))
                .build()

        );

        return TemplateMapper.MapToDto(repo.save(entity));
    }

    public void delete(long id) {
        NotificationTemplate entity = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Template not found"));


        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUser = authentication.getName();

        loggingService.save(NotificationActionsLogs.builder()
                .action("DELETE")
                .details("Deleted Template "+entity.getName())
                .email(currentUser)
                .eventTime(new Timestamp(System.currentTimeMillis()))
                .build()

        );
        repo.delete(entity);
    }

     public Map<String, Object> validateJsonStructure(String jsonString) {
        Map<String, Object> response = new HashMap<>();

        try {
             ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(jsonString);

             if (!jsonNode.has("subject") || jsonNode.get("subject").isNull() ||
                    jsonNode.get("subject").asText().trim().isEmpty()) {
                response.put("valid", false);
                response.put("message", "JSON must contain 'subject' field");
                return response;
            }

             if (!jsonNode.has("body") || jsonNode.get("body").isNull() ||
                    jsonNode.get("body").asText().trim().isEmpty()) {
                response.put("valid", false);
                response.put("message", "JSON must contain 'body' field");
                return response;
            }

            response.put("valid", true);
            response.put("message", "JSON is valid and contains required fields");
            response.put("subject", jsonNode.get("subject").asText());
            response.put("body", jsonNode.get("body").asText());

        } catch (com.fasterxml.jackson.core.JsonParseException e) {
            response.put("valid", false);
            response.put("message", "Invalid JSON syntax: " + e.getOriginalMessage());
            response.put("errorType", "JSON Parse Error");
        } catch (Exception e) {
            response.put("valid", false);
            response.put("message", "JSON processing error: " + e.getMessage());
            response.put("errorType", "General Error");
        }

        return response;
    }

    public List<ReceiverRequest> getAllTemplates(String search) {
        if (search == null || search.isBlank()) {
            return repo.findAll(PageRequest.of(0, 3)) // fetch first 5 if no search term
                    .stream()
                    .map(r->
                            ReceiverRequest.builder().id(r.getId())
                                    .name(r.getName())
                                    .code(r.getCode())
                                    .build())
                    .toList();
        } else {
            return repo.findTop5ByNameContainingIgnoreCase(search).stream().map(r->
                    ReceiverRequest.builder().id(r.getId())
                            .name(r.getName())
                            .code(r.getCode())
                            .build()).toList();
        }
    }
}