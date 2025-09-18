package com.middleware.backend.notification.service;

import com.middleware.backend.notification.dto.ChannelConfigDto;
import com.middleware.backend.notification.enums.ChannelType;
import com.middleware.backend.notification.mapper.ChannelMapper;
import com.middleware.backend.notification.model.ChannelConfig;
import com.middleware.backend.notification.repository.ChannelConfigRepository;
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
public class ChannelService {
    private final ChannelConfigRepository repo;

    public ChannelConfigDto findById(Long id) {
        return repo.findById(id).map(ChannelMapper::mapToDto)
                .orElseThrow(() -> new RuntimeException("Channel not found"));
    }

    public Page<ChannelConfigDto> findAll(Specification<ChannelConfig> spec, Pageable pageable) {
        return repo.findAll(spec, pageable).map(ChannelMapper::mapToDto);
    }

    public ChannelConfigDto create(ChannelConfigDto dto) {
        repo.findByName(dto.getName())
                .ifPresent(c -> { throw new RuntimeException("Channel already exists"); });

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUser = authentication.getName();
        dto.setCreatedBy(currentUser);
        dto.setCreatedAt(new Timestamp(System.currentTimeMillis()));
        dto.setUpdatedBy(currentUser);
        dto.setUpdatedAt(new Timestamp(System.currentTimeMillis()));

        ChannelConfig saved = repo.save(ChannelMapper.mapToEntity(dto));
        return ChannelMapper.mapToDto(saved);
    }

    public ChannelConfigDto update(ChannelConfigDto dto) {
        ChannelConfig entity = repo.findByName(dto.getName())
                .orElseThrow(() -> new RuntimeException("Channel not found"));

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUser = authentication.getName();
        entity.setUpdatedBy(currentUser);
        entity.setUpdatedAt(new Timestamp(System.currentTimeMillis()));
        entity.setActive(dto.isActive());
        entity.setConfig(dto.getConfig());
        entity.setType(ChannelType.valueOf(dto.getType()));

        return ChannelMapper.mapToDto(repo.save(entity));
    }

    public void delete(Long id) {
        if (!repo.existsById(id)) throw new RuntimeException("Channel not found");
        repo.deleteById(id);
    }
}

