package com.middleware.backend.notification.service;

import com.middleware.backend.notification.dto.*;
import com.middleware.backend.notification.mapper.GroupMapper;
import com.middleware.backend.notification.mapper.ReceiverMapper;
import com.middleware.backend.notification.model.NotificationActionsLogs;
import com.middleware.backend.notification.model.NotificationGroup;
import com.middleware.backend.notification.model.Receiver;
import com.middleware.backend.notification.repository.NotificationGroupRepository;
import com.middleware.backend.notification.repository.ReceiverRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for managing Notification Groups.
 * <p>
 * Provides CRUD operations, status toggling, and search capabilities for groups.
 * Logs all create, update, and status-change actions via {@link NotificationActionsLogsService}.
 * </p>
 */
@Service
@RequiredArgsConstructor
public class NotificationGroupService {

    private final NotificationGroupRepository repo;
    private final ReceiverRepository recRepo;
    private final NotificationActionsLogsService loggingService;

    /**
     * Finds a notification group by its ID.
     *
     * @param id the group's ID
     * @return the {@link NotificationGroupDto} representation
     * @throws RuntimeException if the group is not found
     */
    public NotificationGroupDto findById(Long id) {
        return repo.findById(id)
                .map(GroupMapper::mapToDto)
                .orElseThrow(() -> new RuntimeException("Group not found"));
    }

    /**
     * Retrieves paginated groups filtered by a specification.
     *
     * @param spec the filtering specification
     * @param pageable pagination information
     * @return a page of {@link NotificationGroupDto}
     */
    public Page<NotificationGroupDto> findAll(Specification<NotificationGroup> spec, Pageable pageable) {
        return repo.findAll(spec, pageable).map(GroupMapper::mapToDto);
    }

    /**
     * Creates a new notification group.
     *
     * @param dto the group data
     * @return the saved {@link NotificationGroupDto}
     * @throws RuntimeException if a group with the same name or code already exists
     */
    public NotificationGroupDto create(NotificationGroupDto dto) {
        repo.findByName(dto.getName().trim())
                .ifPresent(g -> { throw new RuntimeException("Group Name already exists"); });
        repo.findByCode(dto.getCode().trim())
                .ifPresent(g -> { throw new RuntimeException("Group Code already exists"); });

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUser = authentication.getName();

        dto.setCreatedBy(currentUser);
        dto.setCreatedAt(new Timestamp(System.currentTimeMillis()));
        dto.setUpdatedBy(currentUser);
        dto.setUpdatedAt(new Timestamp(System.currentTimeMillis()));
        dto.setName(dto.getName().trim());
        dto.setCode(dto.getCode().trim());

        NotificationGroup saved = GroupMapper.mapToEntity(dto);
        saved.setReceivers(null); // Initially no receivers
        saved = repo.save(saved);

        loggingService.save(NotificationActionsLogs.builder()
                .action("POST")
                .details("Created New Group " + dto.getName())
                .email(currentUser)
                .eventTime(new Timestamp(System.currentTimeMillis()))
                .build());

        return GroupMapper.mapToDto(saved);
    }

    /**
     * Updates an existing group.
     *
     * @param dto the updated group data
     * @return the updated {@link NotificationGroupDto}
     * @throws RuntimeException if the group is not found
     */
    public NotificationGroupDto update(NotificationGroupDto dto) {
        NotificationGroup entity = repo.findByName(dto.getName().trim())
                .orElseThrow(() -> new RuntimeException("Group not found"));

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUser = authentication.getName();

        entity.setName(dto.getName());
        entity.setDescription(dto.getDescription());
        entity.setActive(dto.isActive());
        entity.setUpdatedBy(currentUser);
        entity.setUpdatedAt(new Timestamp(System.currentTimeMillis()));

        loggingService.save(NotificationActionsLogs.builder()
                .action("PUT")
                .details("Updated Group " + dto.getName())
                .email(currentUser)
                .eventTime(new Timestamp(System.currentTimeMillis()))
                .build());

        return GroupMapper.mapToDto(repo.save(entity));
    }

    /**
     * Toggles the active status of a group.
     *
     * @param id the group's ID
     * @throws RuntimeException if the group is not found
     */
    public void changeStatus(Long id) {
        NotificationGroup entity = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Group not found"));

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUser = authentication.getName();
        if(entity.isActive() && !entity.getReceivers().isEmpty()){
            loggingService.save(NotificationActionsLogs.builder()
                    .action("Failed Inactivating Group")
                    .details("Group is Linked to Receivers, Cant Inactive it")
                    .email(currentUser)
                    .eventTime(new Timestamp(System.currentTimeMillis()))
                    .build());
            throw new RuntimeException("Group is Linked to Receivers, Cant Inactive it");
        }
        loggingService.save(NotificationActionsLogs.builder()
                .action(entity.isActive() ? "Inactivating Group" : "Activating Group")
                .details(entity.isActive() ? "Inactive Group " + entity.getName()
                        : "Activating Group " + entity.getName())
                .email(currentUser)
                .eventTime(new Timestamp(System.currentTimeMillis()))
                .build());

        entity.setUpdatedAt(new Timestamp(System.currentTimeMillis()));
        entity.setUpdatedBy(currentUser);
        entity.setActive(!entity.isActive());

        repo.save(entity);
    }

    /**
     * Searches for active groups, optionally filtered by a search string.
     * Returns at most 5 groups for autocomplete-like functionality.
     *
     * @param search optional search term
     * @return a list of {@link ReceiverRequest} representing the groups
     */
    public List<ReceiverRequest> getAllGroups(String search) {
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
