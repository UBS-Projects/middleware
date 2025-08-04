package com.middleware.backend.service;

import com.middleware.backend.dto.SourceSystemDto;
import com.middleware.backend.mapper.SourceSystemMapper;
import com.middleware.backend.model.SourceSystem;
import com.middleware.backend.repository.SourceSystemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SourceSystemService {

    private final SourceSystemRepository sourceSystemRepository;
    private final SourceSystemMapper sourceSystemMapper;

    public List<SourceSystemDto> getAllSourceSystems() {
        return sourceSystemRepository.findAll().stream()
                .map(sourceSystemMapper::toDto)
                .collect(Collectors.toList());
    }

    public List<SourceSystemDto> getActiveSourceSystems() {
        return sourceSystemRepository.findByActiveTrue().stream()
                .map(sourceSystemMapper::toDto)
                .collect(Collectors.toList());
    }

    public SourceSystemDto getSourceSystemById(Long id) {
        return sourceSystemRepository.findById(id)
                .map(sourceSystemMapper::toDto)
                .orElseThrow(() -> new EntityNotFoundException("Source system not found with id: " + id));
    }

    @Transactional
    public SourceSystemDto createSourceSystem(SourceSystemDto dto) {
        if (sourceSystemRepository.existsByNameIgnoreCase(dto.getName())) {
            throw new IllegalArgumentException("Source system with name '" + dto.getName() + "' already exists.");
        }
        SourceSystem sourceSystem = sourceSystemMapper.toEntity(dto);
        SourceSystem savedSourceSystem = sourceSystemRepository.save(sourceSystem);
        return sourceSystemMapper.toDto(savedSourceSystem);
    }

    @Transactional
    public SourceSystemDto updateSourceSystem(Long id, SourceSystemDto dto) {
        SourceSystem existingSourceSystem = sourceSystemRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Source system not found with id: " + id));

        // Check if name is changing and if new name already exists
        if (!existingSourceSystem.getName().equalsIgnoreCase(dto.getName()) &&
                sourceSystemRepository.existsByNameIgnoreCase(dto.getName())) {
            throw new IllegalArgumentException("Source system with name '" + dto.getName() + "' already exists.");
        }

        existingSourceSystem.setName(dto.getName());
        existingSourceSystem.setDescription(dto.getDescription());
        existingSourceSystem.setActive(dto.getActive());

        SourceSystem updatedSourceSystem = sourceSystemRepository.save(existingSourceSystem);
        return sourceSystemMapper.toDto(updatedSourceSystem);
    }

    @Transactional
    public void deleteSourceSystem(Long id) {
        if (!sourceSystemRepository.existsById(id)) {
            throw new EntityNotFoundException("Source system not found with id: " + id);
        }

        // --==-- حماية مهمة جداً --==--
        // لا تسمح بحذف source system مستخدم في أي ErrorMapping
        long usageCount = sourceSystemRepository.countErrorMappingsBySourceSystemId(id);
        if (usageCount > 0) {
            throw new IllegalStateException("Cannot delete source system with id " + id + " because it is used by " + usageCount + " error mapping(s).");
        }
        sourceSystemRepository.deleteById(id);
    }

    @Transactional
    public SourceSystemDto toggleSourceSystem(Long id) {
        SourceSystem sourceSystem = sourceSystemRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Source system not found with id: " + id));

        sourceSystem.setActive(!sourceSystem.getActive());
        SourceSystem savedSourceSystem = sourceSystemRepository.save(sourceSystem);
        return sourceSystemMapper.toDto(savedSourceSystem);
    }
}