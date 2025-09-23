package com.middleware.backend.notification.service;

import com.middleware.backend.notification.dto.ReceiverDto;
import com.middleware.backend.notification.mapper.ReceiverMapper;
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

    public ReceiverDto findById(Long id) {
        return repo.findById(id).map(ReceiverMapper::mapToDto)
                .orElseThrow(() -> new RuntimeException("Receiver not found"));
    }

    public Page<ReceiverDto> findAll(Pageable pageable) {
        return repo.findAll(pageable).map(ReceiverMapper::mapToDto);
    }

    public ReceiverDto create(ReceiverDto dto) {
        repo.findByPhone(dto.getPhone().trim())
                .ifPresent(r -> { throw new RuntimeException("Phone already in use"); });

        repo.findByEmail(dto.getEmail().trim())
                .ifPresent(r -> { throw new RuntimeException("Email already in use"); });

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUser = authentication.getName();
        dto.setCreatedBy(currentUser);
        dto.setCreatedAt(new Timestamp(System.currentTimeMillis()));
        dto.setUpdatedBy(currentUser);
        dto.setUpdatedAt(new Timestamp(System.currentTimeMillis()));
        dto.setName(dto.getName().trim());
        dto.setEmail(dto.getEmail().trim());
        dto.setEmail(dto.getEmail().trim());
        Receiver saved = repo.save(ReceiverMapper.mapToEntity(dto));
        return ReceiverMapper.mapToDto(saved);
    }

    public ReceiverDto update(ReceiverDto dto) {
        Receiver entity = repo.findById(dto.getId())
                .orElseThrow(() -> new RuntimeException("Receiver not found"));
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUser = authentication.getName();
        entity.setName(dto.getName().trim());
        entity.setUpdatedBy(currentUser);
        entity.setUpdatedAt(new Timestamp(System.currentTimeMillis()));

        return ReceiverMapper.mapToDto(repo.save(entity));
    }

    public void delete(Long id) {
        if (!repo.existsById(id)) throw new RuntimeException("Receiver not found");
        repo.deleteById(id);
    }
}

