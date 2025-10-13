package com.middleware.backend.notification.service;

import com.middleware.backend.notification.dto.*;
import com.middleware.backend.notification.mapper.GroupMapper;
import com.middleware.backend.notification.model.NotificationActionsLogs;
import com.middleware.backend.notification.model.NotificationGroup;
import com.middleware.backend.notification.model.Receiver;
import com.middleware.backend.notification.repository.NotificationGroupRepository;
import com.middleware.backend.notification.repository.ReceiverRepository;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service for managing receivers within notification groups.
 * <p>
 * Handles adding receivers to groups, retrieving unlinked groups, listing group receivers,
 * and updating group-receiver associations. Logs all actions using
 * {@link NotificationActionsLogsService}.
 * </p>
 */
@Service
@AllArgsConstructor
public class groupReceiverService {

    private final NotificationGroupRepository groupRepo;
    private final ReceiverRepository receiverRepo;
    private final NotificationActionsLogsService loggingService;

    /**
     * Adds receivers to a notification group.
     *
     * @param dto the {@link GroupReceiversDto} containing group ID and list of receiver IDs
     * @return the updated {@link NotificationGroupDto} with receivers linked
     * @throws RuntimeException if the group or any receiver is not found
     */
    public NotificationGroupDto addReceivers(GroupReceiversDto dto) {
        NotificationGroup entity = groupRepo.findById(dto.getId())
                .orElseThrow(() -> new RuntimeException("Group not found"));

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUser = authentication.getName();
        entity.setUpdatedBy(currentUser);
        entity.setUpdatedAt(new Timestamp(System.currentTimeMillis()));

        List<Receiver> receivers = new ArrayList<>();
        for (Long r : dto.getReceivers()) {
            receivers.add(receiverRepo.findById(r).orElseThrow(() -> new RuntimeException("Receiver not found")));
        }
        entity.setReceivers(receivers);

        String recNames = receivers.stream()
                .map(Receiver::getName)
                .collect(Collectors.joining(","));

        // Log action
        loggingService.save(NotificationActionsLogs.builder()
                .action("POST")
                .details("Linked Receivers: " + recNames + " Into Group " + entity.getName())
                .email(currentUser)
                .eventTime(new Timestamp(System.currentTimeMillis()))
                .build()
        );

        return GroupMapper.mapToDto(groupRepo.save(entity));
    }

    /**
     * Retrieves a list of receivers for dropdown/search purposes.
     *
     * @param search optional search term
     * @return a list of {@link ReceiverRequest} containing receiver IDs and names
     */
    public List<ReceiverRequest> getReceivers(String search) {
        if (search == null || search.isBlank()) {
            return receiverRepo.findAll(PageRequest.of(0, 3))
                    .stream()
                    .map(r -> ReceiverRequest.builder()
                            .id(r.getId())
                            .name(r.getName())
                            .build())
                    .toList();
        } else {
            return receiverRepo.findTop5ByNameContainingIgnoreCase(search)
                    .stream()
                    .map(r -> ReceiverRequest.builder()
                            .id(r.getId())
                            .name(r.getName())
                            .build())
                    .toList();
        }
    }

    /**
     * Retrieves groups that do not have any receivers linked.
     *
     * @return a list of {@link GroupRequest} for unlinked groups
     */
    public List<GroupRequest> getUnlinkedGroups() {
        return groupRepo.findAllWithoutReceivers()
                .stream()
                .map(r -> GroupRequest.builder()
                        .id(r.getId())
                        .groupName(r.getName())
                        .build())
                .toList();
    }

    /**
     * Retrieves a paginated list of groups with their linked receivers.
     *
     * @param pageable pagination information
     * @return a page of {@link GroupReceiversDto2} containing group name and concatenated receiver names
     */
    public Page<GroupReceiversDto2> getGroupReceivers(Specification<NotificationGroup> spec, Pageable pageable) {
        return groupRepo.findAll(spec, pageable)  // use spec directly
                .map(g -> GroupReceiversDto2.builder()
                        .groupId(g.getId())
                        .groupName(g.getName())
                        .receiverName(g.getReceivers().stream()
                                .map(Receiver::getName)
                                .collect(Collectors.joining(", ")))
                        .build());
    }

    /**
     * Retrieves a notification group by its name.
     *
     * @param groupName the name of the group
     * @return the {@link NotificationGroup} entity
     * @throws NoSuchElementException if the group is not found
     */
    public NotificationGroup getGroup(String groupName) {
        return groupRepo.findByName(groupName).orElseThrow();
    }

    /**
     * Updates receivers for a notification group.
     *
     * @param body the {@link GroupReceiversDto} containing group ID and updated receiver IDs
     * @return the updated {@link NotificationGroup} entity
     * @throws ResponseStatusException if the group is not found
     */
    public ResponseEntity<?> updateGroup(GroupReceiversDto body) {
        Optional<NotificationGroup> exists = groupRepo.findById(body.getId());
        if (exists.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        exists.get().setReceivers(null);
        if(body.getReceivers().isEmpty()){
            return ResponseEntity.ok(groupRepo.save(exists.get()));
        }
        List<Receiver> receivers = new ArrayList<>();
        for (Long receiverId : body.getReceivers()) {
            Receiver newReceiver = receiverRepo.findById(receiverId)
                    .orElseThrow(() -> new RuntimeException("Receiver not found"));
            receivers.add(newReceiver);
        }

        exists.get().setReceivers(receivers);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUser = authentication.getName();

        // Log action
        loggingService.save(NotificationActionsLogs.builder()
                .action("PUT")
                .details("Updated Group Receivers for " + exists.get().getName())
                .email(currentUser)
                .eventTime(new Timestamp(System.currentTimeMillis()))
                .build()
        );

        return ResponseEntity.ok(groupRepo.save(exists.get()));
    }
}
