package com.middleware.backend.notification.service;

import com.middleware.backend.notification.dto.ReceiverDto;
import com.middleware.backend.notification.mapper.ReceiverMapper;
import com.middleware.backend.notification.model.NotificationActionsLogs;
import com.middleware.backend.notification.model.Receiver;
import com.middleware.backend.notification.repository.ReceiverRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import java.sql.Timestamp;

@Service
@RequiredArgsConstructor
public class ReceiverService {

    private final ReceiverRepository repo;
    private final NotificationActionsLogsService loggingService;

    public ReceiverDto findById(Long id) {
        return repo.findById(id)
                .map(ReceiverMapper::mapToDto)
                .orElseThrow(() -> new RuntimeException("Receiver not found"));
    }

    public Page<ReceiverDto> findAll(Specification<Receiver> spec, Pageable pageable) {
        return repo.findAll(spec, pageable).map(ReceiverMapper::mapToDto);
    }

    public ReceiverDto create(ReceiverDto dto) {
        repo.findByPhone(dto.getPhone().trim())
                .ifPresent(r -> { throw new RuntimeException("Phone already in use"); });

        repo.findByEmail(dto.getEmail().trim())
                .ifPresent(r -> { throw new RuntimeException("Email already in use"); });

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUser = authentication.getName();

        Timestamp now = new Timestamp(System.currentTimeMillis());

        dto.setName(dto.getName().trim());
        dto.setEmail(dto.getEmail().trim());
        dto.setCreatedBy(currentUser);
        dto.setCreatedAt(now);
        dto.setUpdatedBy(currentUser);
        dto.setUpdatedAt(now);

        Receiver saved = repo.save(ReceiverMapper.mapToEntity(dto));

        loggingService.save(NotificationActionsLogs.builder()
                .action("POST")
                .details("Created New Receiver " + dto.getName())
                .email(currentUser)
                .eventTime(now)
                .build()
        );

        return ReceiverMapper.mapToDto(saved);
    }

    public ReceiverDto update(ReceiverDto dto) {
        Receiver entity = repo.findById(dto.getId())
                .orElseThrow(() -> new RuntimeException("Receiver not found"));

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUser = authentication.getName();
        Timestamp now = new Timestamp(System.currentTimeMillis());

        entity.setName(dto.getName().trim());
        entity.setUpdatedBy(currentUser);
        entity.setUpdatedAt(now);

        loggingService.save(NotificationActionsLogs.builder()
                .action("PUT")
                .details("Updated Receiver " + dto.getName())
                .email(currentUser)
                .eventTime(now)
                .build()
        );

        return ReceiverMapper.mapToDto(repo.save(entity));
    }
}


