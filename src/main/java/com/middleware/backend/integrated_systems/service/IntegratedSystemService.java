package com.middleware.backend.integrated_systems.service;

import com.middleware.backend.integrated_systems.dto.IntegratedSystemDto;
import com.middleware.backend.integrated_systems.mapper.IntegratedSystemMapper;
import com.middleware.backend.integrated_systems.model.IntegratedSystem;
import com.middleware.backend.integrated_systems.repository.IntegratedSystemRepository;
import com.middleware.backend.users.model.User;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.Optional;

@Service
@AllArgsConstructor
public class IntegratedSystemService {
    private final IntegratedSystemRepository repo;

    public Page<?> getAll(Specification<IntegratedSystem> spec, Pageable pageable) {
        Page<IntegratedSystem> page = repo.findAll(spec, pageable);
        return page.map(IntegratedSystemMapper::toDto);
    }


    public IntegratedSystemDto create(IntegratedSystemDto body) {
        Optional<IntegratedSystem> exists = repo.findByCode(body.getCode());
        if (exists.isPresent())
            return null;
        IntegratedSystem entity = IntegratedSystemMapper.toEntity(body);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String emailUser = authentication.getName();
        entity.setCreatedBy(emailUser);
        entity.setCreatedAt(new Timestamp(System.currentTimeMillis()));
        entity.setUpdatedBy(emailUser);
        entity.setUpdatedAt(new Timestamp(System.currentTimeMillis()));
        return IntegratedSystemMapper.toDto(repo.save(entity));
    }

    public IntegratedSystemDto update(IntegratedSystemDto body) {
        Optional<IntegratedSystem> exists = repo.findByCode(body.getCode());
        if (exists.isEmpty())
            return null;
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String emailUser = authentication.getName();
        exists.get().setUpdatedBy(emailUser);
        exists.get().setUpdatedAt(new Timestamp(System.currentTimeMillis()));
        exists.get().setHost(body.getHost());
        exists.get().setPort(body.getPort());
        exists.get().setDescription(body.getDescription());
        exists.get().setProtocol(body.getProtocol());
        exists.get().setAdditionalValue1(body.getAdditionalValue1());
        exists.get().setAdditionalKey1(body.getAdditionalKey1());
        exists.get().setAdditionalValue2(body.getAdditionalValue2());
        exists.get().setAdditionalKey2(body.getAdditionalKey2());
        exists.get().setAuthenticationType(body.getAuthenticationType());
        exists.get().setUsername(body.getUsername());
        exists.get().setPassword(body.getPassword());
        exists.get().setToken(body.getToken());
        return IntegratedSystemMapper.toDto(repo.save(exists.get()));
    }

    public IntegratedSystemDto getById(String code ) {
        return repo.findByCode(code).map(IntegratedSystemMapper::toDto).orElse(null);
    }
}
