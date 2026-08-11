package com.middleware.backend.integrated_systems.service;

import com.middleware.backend.integrated_systems.dto.IntegratedSystemDto;
import com.middleware.backend.integrated_systems.mapper.IntegratedSystemMapper;
import com.middleware.backend.integrated_systems.model.IntegratedSystem;
import com.middleware.backend.integrated_systems.repository.IntegratedSystemRepository;
import com.middleware.backend.integrated_systems.validation.IntegratedSystemValidator;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service class for managing {@link IntegratedSystem} entities.
 * <p>
 * Provides methods to create, update, retrieve, and list integrated systems.
 */
@Service
@AllArgsConstructor
public class IntegratedSystemService {

    private final IntegratedSystemRepository repo;

    /**
     * Retrieves a paginated list of integrated systems matching the given specification.
     *
     * @param spec     JPA specification for filtering
     * @param pageable pagination and sorting information
     * @return a page of {@link IntegratedSystemDto} matching the specification
     */
    public Page<IntegratedSystemDto> getAll(Specification<IntegratedSystem> spec, Pageable pageable) {
        Page<IntegratedSystem> page = repo.findAll(spec, pageable);
        return page.map(IntegratedSystemMapper::toDto);
    }
    /**
     * Retrieves a list of all integrated system codes for dropdown usage.
     * Returns only codes sorted alphabetically.
     *
     * @return list of system codes
     */
    public List<String> getAllCodes() {
        return repo.findAll().stream()
                .map(IntegratedSystem::getCode)
                .sorted()
                .collect(Collectors.toList());
    }
    /**
     * Creates a new integrated system if a system with the same code does not already exist.
     *
     * @param body the DTO containing details of the system to create
     * @return the created {@link IntegratedSystemDto}, or null if a system with the same code already exists
     */
    public IntegratedSystemDto create(IntegratedSystemDto body) {
        IntegratedSystemValidator.applyDefaultsAndValidate(body);
        Optional<IntegratedSystem> exists = repo.findByCode(body.getCode());
        if (exists.isPresent())
            return null;

        IntegratedSystem entity = IntegratedSystemMapper.toEntity(body);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String emailUser = authentication.getName();

        Timestamp now = new Timestamp(System.currentTimeMillis());
        entity.setCreatedBy(emailUser);
        entity.setCreatedAt(now);
        entity.setUpdatedBy(emailUser);
        entity.setUpdatedAt(now);
        entity.setId(null);

        return IntegratedSystemMapper.toDto(repo.save(entity));
    }

    /**
     * Updates an existing integrated system.
     *
     * @param body the DTO containing updated system details
     * @return the updated {@link IntegratedSystemDto}, or null if the system with the given code does not exist
     */
    public IntegratedSystemDto update(IntegratedSystemDto body) {
        IntegratedSystemValidator.applyDefaultsAndValidate(body);
        Optional<IntegratedSystem> exists = repo.findByCode(body.getCode());
        if (exists.isEmpty()) return null;

        IntegratedSystem entity = exists.get();
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String emailUser = authentication.getName();

        entity.setUpdatedBy(emailUser);
        entity.setUpdatedAt(new Timestamp(System.currentTimeMillis()));
        entity.setHost(body.getHost());
        entity.setPort(body.getPort());
        entity.setDescription(body.getDescription());
        entity.setSystemType(body.getSystemType());
        entity.setProtocol(body.getProtocol());
        entity.setAdditionalKey1(body.getAdditionalKey1());
        entity.setAdditionalValue1(body.getAdditionalValue1());
        entity.setAdditionalKey2(body.getAdditionalKey2());
        entity.setAdditionalValue2(body.getAdditionalValue2());
        entity.setAuthenticationType(body.getAuthenticationType());
        entity.setUsername(body.getUsername());
        entity.setPassword(body.getPassword());
        entity.setToken(body.getToken());

        return IntegratedSystemMapper.toDto(repo.save(entity));
    }

    /**
     * Retrieves a single integrated system by its unique code.
     *
     * @param code the unique code of the integrated system
     * @return the corresponding {@link IntegratedSystemDto}, or null if not found
     */
    public IntegratedSystemDto getById(String code) {
        return repo.findByCode(code)
                .map(IntegratedSystemMapper::toDto)
                .orElse(null);
    }
}
