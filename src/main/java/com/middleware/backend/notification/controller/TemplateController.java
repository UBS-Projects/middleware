package com.middleware.backend.notification.controller;

import com.middleware.backend.notification.dto.NotificationTemplateDto;
import com.middleware.backend.notification.enums.ChannelType;
import com.middleware.backend.notification.model.NotificationTemplate;
import com.middleware.backend.notification.service.NotificationTemplateService;
import com.middleware.backend.notification.specification.NotificationTemplateSpecification;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.time.LocalDate;
/**
 * REST Controller for managing notification templates.
 *
 * <p>This controller handles all operations related to notification templates,
 * including creation, update, retrieval, validation, and filtering.
 * It supports pagination, sorting, and filtering based on template fields and metadata.</p>
 *
 * <p>Endpoints include:
 * <ul>
 *   <li>GET /api/templates/{id} - Retrieve a template by ID</li>
 *   <li>GET /api/templates - Retrieve all templates with filtering and pagination</li>
 *   <li>POST /api/templates - Create a new template</li>
 *   <li>PUT /api/templates - Update an existing template</li>
 *   <li>PATCH /api/templates/{id} - Toggle a template's active status</li>
 *   <li>POST /api/templates/validate-json - Validate the JSON structure of a template</li>
 *   <li>GET /api/templates/all - Retrieve all templates for dropdowns or lookups</li>
 * </ul></p>
 *
 * <p>All endpoints are secured using role-based permissions via Spring Security.</p>
 */
@RestController
@RequestMapping("/api/templates")
@AllArgsConstructor
public class TemplateController {

    private final NotificationTemplateService service;

    /**
     * Retrieves a specific notification template by its unique ID.
     *
     * @param id The ID of the notification template.
     * @return ResponseEntity containing the template details.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('template:view')")
    public ResponseEntity<?> get(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }

    /**
     * Retrieves all templates with optional filters, pagination, and sorting.
     *
     * <p>Supported filters include:
     * <ul>
     *   <li><b>templateName</b> – partial match on template name</li>
     *   <li><b>code</b> – partial match on template code</li>
     *   <li><b>type</b> – filter by channel type (e.g., EMAIL, SMS, PUSH)</li>
     *   <li><b>createdAfter</b> / <b>createdBefore</b> – filter by creation date range</li>
     * </ul></p>
     *
     * @param templateName Optional filter for the template name.
     * @param code Optional filter for the template code.
     * @param type Optional filter for channel type (EMAIL, SMS, PUSH, etc.).
     * @param createdAfter Filter for templates created after this date.
     * @param createdBefore Filter for templates created before this date.
     * @param page Page index for pagination (default 0).
     * @param size Number of items per page (default 10).
     * @param sortedBy Field to sort by (default "updatedAt").
     * @param sortDirection Sort order ("asc" or "desc", default "desc").
     * @return Paginated list of templates matching the criteria.
     */
    @GetMapping("")
    @PreAuthorize("hasAuthority('template:view')")
    public ResponseEntity<?> getAll(
            @RequestParam(required = false) String templateName,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdAfter,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdBefore,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false, defaultValue = "updatedAt") String sortedBy,
            @RequestParam(defaultValue = "desc") String sortDirection
    ) {
        Pageable pageable = PageRequest.of(page, size,
                sortDirection.equalsIgnoreCase("asc")
                        ? Sort.by(sortedBy).ascending()
                        : Sort.by(sortedBy).descending());

        // Convert string 'type' to ChannelType enum safely (ignore invalid values)
        ChannelType channelType = null;
        if (type != null) {
            try {
                channelType = ChannelType.valueOf(type.toUpperCase());
            } catch (IllegalArgumentException ignored) {
                // invalid type string, ignored
            }
        }

        // Build dynamic JPA specification for filtering
        Specification<NotificationTemplate> spec = Specification
                .where(NotificationTemplateSpecification.hasField("name", templateName, NotificationTemplateSpecification.MatchMode.CONTAINS))
                .and(NotificationTemplateSpecification.hasField("code", code, NotificationTemplateSpecification.MatchMode.CONTAINS))
                .and(NotificationTemplateSpecification.hasType(channelType))
                .and(NotificationTemplateSpecification.dateAfter("createdAt", createdAfter))
                .and(NotificationTemplateSpecification.dateBefore("createdAt", createdBefore));

        return ResponseEntity.ok(service.findAll(spec, pageable));
    }

    /**
     * Creates a new notification template.
     *
     * @param dto The DTO object containing template data such as name, code, content, and type.
     * @return ResponseEntity containing the created template.
     */
    @PostMapping("")
    @PreAuthorize("hasAuthority('template:create')")
    public ResponseEntity<?> create(@Valid @RequestBody NotificationTemplateDto dto) {
        return ResponseEntity.ok(service.create(dto));
    }

    /**
     * Updates an existing notification template.
     *
     * @param dto The DTO object containing updated template information.
     * @return ResponseEntity containing the updated template.
     */
    @PutMapping("")
    @PreAuthorize("hasAuthority('template:edit')")
    public ResponseEntity<?> update(@Valid @RequestBody NotificationTemplateDto dto) {
        return ResponseEntity.ok(service.update(dto));
    }

    /**
     * Toggles the activation status of a template (e.g., active/inactive).
     *
     * @param id The template ID.
     * @return HTTP 204 No Content on success.
     */
    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('template:edit')")
    public ResponseEntity<?> changeStatus(@PathVariable long id) {
        service.changeStatus(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Validates the structure of a provided JSON string for template body format correctness.
     *
     * @param jsonString Raw JSON content to validate.
     * @return ResponseEntity containing validation results.
     */
    @PostMapping("/validate-json")
    @PreAuthorize("hasAuthority('template:validate')")
    public ResponseEntity<?> validateJsonStructure(@RequestBody String jsonString) {
        return ResponseEntity.ok(service.validateJsonStructure(jsonString));
    }

    /**
     * Retrieves all templates for quick dropdown lists or search suggestions.
     *
     * @param search Optional text filter to search templates by name or code.
     * @return List of all templates matching the search query.
     */
    @GetMapping("/all")
    @PreAuthorize("hasAuthority('template:view')")
    public ResponseEntity<?> getAllTemplates(@RequestParam(required = false) String search) {
        return ResponseEntity.ok(service.getAllTemplates(search));
    }
}