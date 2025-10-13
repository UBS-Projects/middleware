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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class NotificationTemplateService {

    private final TemplateRepository repo;
    private final NotificationActionsLogsService loggingService;

    public NotificationTemplateDto findById(Long id) {
        return repo.findById(id)
                .map(TemplateMapper::MapToDto)
                .orElseThrow(() -> new RuntimeException("Template not found"));
    }

    public Page<NotificationTemplateDto> findAll(Specification<NotificationTemplate> spec, Pageable pageable) {
        return repo.findAll(spec, pageable).map(TemplateMapper::MapToDto);
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
                .details("Created New Template " + dto.getName())
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
        entity.setActive(dto.isActive());
        entity.setUpdatedBy(currentUser);
        entity.setUpdatedAt(new Timestamp(System.currentTimeMillis()));

        loggingService.save(NotificationActionsLogs.builder()
                .action("PUT")
                .details("Updated Template " + dto.getName())
                .email(currentUser)
                .eventTime(new Timestamp(System.currentTimeMillis()))
                .build()
        );

        return TemplateMapper.MapToDto(repo.save(entity));
    }

    public void changeStatus(long id) {
        NotificationTemplate entity = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Template not found"));

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUser = authentication.getName();

        loggingService.save(NotificationActionsLogs.builder()
                .action(entity.isActive() ? "Inactivating Template" : "Activating Template")
                .details(entity.isActive() ? "Inactive Template " + entity.getName() : "Activating Template " + entity.getName())
                .email(currentUser)
                .eventTime(new Timestamp(System.currentTimeMillis()))
                .build()
        );

        entity.setActive(!entity.isActive());
        entity.setUpdatedBy(currentUser);
        entity.setUpdatedAt(new Timestamp(System.currentTimeMillis()));
        repo.save(entity);
    }

    public Map<String, Object> validateJsonStructure(String jsonString) {
        Map<String, Object> response = new HashMap<>();
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(jsonString);

            // Check required fields
            if (!jsonNode.has("subject") || jsonNode.get("subject").isNull() ||
                    jsonNode.get("subject").asText().trim().isEmpty()) {
                response.put("valid", false);
                response.put("message", "JSON must contain a non-empty 'subject' field");
                return response;
            }

            if (!jsonNode.has("body") || jsonNode.get("body").isNull() ||
                    jsonNode.get("body").asText().trim().isEmpty()) {
                response.put("valid", false);
                response.put("message", "JSON must contain a non-empty 'body' field");
                return response;
            }

            String subject = jsonNode.get("subject").asText();
            String body = jsonNode.get("body").asText();

            // Validate placeholders
            String placeholderError = validatePlaceholders(subject, "subject");
            if (placeholderError != null) {
                response.put("valid", false);
                response.put("message", placeholderError);
                return response;
            }

            placeholderError = validatePlaceholders(body, "body");
            if (placeholderError != null) {
                response.put("valid", false);
                response.put("message", placeholderError);
                return response;
            }

            // ✅ Everything passed
            response.put("valid", true);
            response.put("message", "JSON is valid and contains required fields with correct placeholders");
            response.put("subject", subject);
            response.put("body", body);

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

    private String validatePlaceholders(String text, String fieldName) {
        // Match all double-curly placeholders
        Pattern doubleBracePattern = Pattern.compile("\\{\\{([^{}]+)\\}\\}");
        Matcher matcher = doubleBracePattern.matcher(text);

        // Check for malformed double-brace placeholders
        while (matcher.find()) {
            String inside = matcher.group(1).trim();
            if (inside.isEmpty()) {
                return String.format(
                        "Empty placeholder detected in '%s'. Use {{name}} format.",
                        fieldName
                );
            }
            // Optional: add character validation if needed
            if (!inside.matches("[a-zA-Z0-9_.]+")) {
                return String.format(
                        "Invalid characters in placeholder '%s' in '%s'. Use letters, numbers, underscore or dot only.",
                        inside, fieldName
                );
            }
        }

        // Now check for any unmatched '{{' or '}}' (e.g., '{{name' or 'name}}')
        int openBraces = text.length() - text.replace("{{", "").length();
        int closeBraces = text.length() - text.replace("}}", "").length();
        if (openBraces != closeBraces) {
            return String.format(
                    "Unbalanced double braces in '%s'. Ensure all '{{' have matching '}}'.",
                    fieldName
            );
        }

        return null; // All good
    }


    public List<ReceiverRequest> getAllTemplates(String search) {
        if (search == null || search.isBlank()) {
            return repo.findAllByActiveTrue(PageRequest.of(0, 3))
                    .stream()
                    .map(r -> ReceiverRequest.builder()
                            .id(r.getId())
                            .name(r.getName())
                            .code(r.getCode())
                            .build())
                    .toList();
        } else {
            return repo.findTop5ByNameContainingIgnoreCaseAndActiveTrue(search)
                    .stream()
                    .map(r -> ReceiverRequest.builder()
                            .id(r.getId())
                            .name(r.getName())
                            .code(r.getCode())
                            .build())
                    .toList();
        }
    }
}
