package com.middleware.backend.notification.service;

import com.middleware.backend.notification.dto.*;
import com.middleware.backend.notification.mapper.GroupMapper;
import com.middleware.backend.notification.mapper.ReceiverMapper;
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

    public NotificationGroupDto findById(Long id) {
        return repo.findById(id).map(GroupMapper::mapToDto)
                .orElseThrow(() -> new RuntimeException("Group not found"));
    }

    public Page<NotificationGroupDto> findAll(Pageable pageable) {
        return repo.findAll(pageable).map(GroupMapper::mapToDto);
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
        return GroupMapper.mapToDto(saved);
    }

    public NotificationGroupDto update(NotificationGroupDto dto) {
        NotificationGroup entity = repo.findByName(dto.getName().trim())
                .orElseThrow(() -> new RuntimeException("Group not found"));

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUser = authentication.getName();
        entity.setName(dto.getName());
        entity.setDescription(dto.getDescription());
        entity.setUpdatedBy(currentUser);
        entity.setUpdatedAt(new Timestamp(System.currentTimeMillis()));

        return GroupMapper.mapToDto(repo.save(entity));
    }

    public void delete(Long id) {
        if (!repo.existsById(id)) throw new RuntimeException("Group not found");
        repo.deleteById(id);
    }


    public List<ReceiverRequest> getReceivers(String search) {
        if (search == null || search.isBlank()) {
            return repo.findAll(PageRequest.of(0, 3)) // fetch first 5 if no search term
                    .stream()
                    .map(r->
                            ReceiverRequest.builder().id(r.getId())
                                    .name(r.getName())
                                    .build())
                    .toList();
        } else {
            return repo.findTop5ByNameContainingIgnoreCase(search).stream().map(r->
                    ReceiverRequest.builder().id(r.getId())
                            .name(r.getName())
                            .build()).toList();
        }
    }
}
