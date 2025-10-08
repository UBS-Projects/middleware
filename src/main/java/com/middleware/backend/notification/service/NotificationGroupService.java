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

@Service
@RequiredArgsConstructor
public class NotificationGroupService {
    private final NotificationGroupRepository repo;
    private final ReceiverRepository recRepo;
    private final NotificationActionsLogsService loggingService;

    public NotificationGroupDto findById(Long id) {
        return repo.findById(id).map(GroupMapper::mapToDto)
                .orElseThrow(() -> new RuntimeException("Group not found"));
    }

    public Page<NotificationGroupDto> findAll(Specification<NotificationGroup> spec, Pageable pageable) {
        return repo.findAll(spec, pageable).map(GroupMapper::mapToDto);
    }

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
        saved.setReceivers(null);
        saved = repo.save(saved);

        loggingService.save(NotificationActionsLogs.builder()
                .action("POST")
                .details("Created New Group "+dto.getName())
                .email(currentUser)
                .eventTime(new Timestamp(System.currentTimeMillis()))
                .build()

        );
        return GroupMapper.mapToDto(saved);
    }

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
                .details("Updated Group "+dto.getName())
                .email(currentUser)
                .eventTime(new Timestamp(System.currentTimeMillis()))
                .build()
        );
        return GroupMapper.mapToDto(repo.save(entity));
    }

    public void changeStatus(Long id) {
        NotificationGroup entity = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Template not found"));


        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUser = authentication.getName();
        loggingService.save(NotificationActionsLogs.builder()
                .action(entity.isActive()?"Inactivating Group": "Activating Group ")
                .details(entity.isActive()?"Inactive Group "+entity.getName() : "Activating Group "+entity.getName())
                .email(currentUser)
                .eventTime(new Timestamp(System.currentTimeMillis()))
                .build()

        );
        entity.setUpdatedAt(new Timestamp(System.currentTimeMillis()));
        entity.setUpdatedBy(currentUser);
        entity.setActive(!entity.isActive());
        repo.save(entity);
    }


    public List<ReceiverRequest> getAllGroups(String search) {
        if (search == null || search.isBlank()) {
            return repo.findAllByActiveTrue(PageRequest.of(0, 3)) // fetch first 5 if no search term
                    .stream()
                    .map(r->
                            ReceiverRequest.builder().id(r.getId())
                                    .name(r.getName())
                                    .code(r.getCode())
                                    .build())
                    .toList();
        } else {
            return repo.findTop5ByNameContainingIgnoreCaseAndActiveTrue(search).stream().map(r->
                    ReceiverRequest.builder().id(r.getId())
                            .name(r.getName())
                            .code(r.getCode())
                            .build()).toList();
        }
    }
}
