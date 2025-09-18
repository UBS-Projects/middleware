package com.middleware.backend.notification.service;

import com.middleware.backend.notification.dto.NotificationTemplateDto;
import com.middleware.backend.notification.enums.ChannelType;
import com.middleware.backend.notification.mapper.TemplateMapper;
import com.middleware.backend.notification.model.NotificationTemplate;
import com.middleware.backend.notification.repository.TemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import java.sql.Timestamp;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class NotificationTemplateService {
    private final TemplateRepository repo;

    public NotificationTemplateDto findById(Long id) {
        return repo.findById(id).map(TemplateMapper::MapToDto)
                .orElseThrow(() -> new RuntimeException("Template not found"));
    }

    public Page<NotificationTemplateDto> findAll( Pageable pageable) {
        return repo.findAll(pageable).map(TemplateMapper::MapToDto);
    }

    public NotificationTemplateDto create(NotificationTemplateDto dto) {
        repo.findByName(dto.getName())
                .ifPresent(t -> { throw new RuntimeException("Template already exists"); });

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUser = authentication.getName();
        dto.setCreatedBy(currentUser);
        dto.setCreatedAt(new Timestamp(System.currentTimeMillis()));
        dto.setUpdatedBy(currentUser);
        dto.setUpdatedAt(new Timestamp(System.currentTimeMillis()));

        NotificationTemplate saved = repo.save(TemplateMapper.MapToEntity(dto));
        return TemplateMapper.MapToDto(saved);
    }

    public NotificationTemplateDto update(NotificationTemplateDto dto) {
        NotificationTemplate entity = repo.findById(dto.getId())
                .orElseThrow(() -> new RuntimeException("Template not found"));

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUser = authentication.getName();
        entity.setBody(dto.getBody());
        entity.setSubject(dto.getSubject());
        entity.setType(ChannelType.valueOf(dto.getType()));
        entity.setUpdatedBy(currentUser);
        entity.setUpdatedAt(new Timestamp(System.currentTimeMillis()));

        return TemplateMapper.MapToDto(repo.save(entity));
    }

    public void delete(long id) {
        NotificationTemplate entity = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Template not found"));
        repo.delete(entity);
    }
}
