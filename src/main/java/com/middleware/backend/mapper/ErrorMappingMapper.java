package com.middleware.backend.mapper;

import org.springframework.stereotype.Component;

import com.middleware.backend.dto.ErrorMappingDto;
import com.middleware.backend.model.DestinationApi;
import com.middleware.backend.model.ErrorMapping;
import com.middleware.backend.repository.DestinationApiRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ErrorMappingMapper {

    private final DestinationApiRepository destinationApiRepository;

    public ErrorMappingDto toDto(ErrorMapping entity) {
        return ErrorMappingDto.builder()
                .id(entity.getId())
                .destinationApiId(entity.getDestinationApi().getId())
                .destinationSystemName(entity.getDestinationSystemName())
                .rawErrorSubstring(entity.getRawErrorSubstring())
                .matchType(entity.getMatchType())
                .mappedErrorCode(entity.getMappedErrorCode())
                .mappedMessage(entity.getMappedMessage())
                .errorCategory(entity.getErrorCategory())
                .httpStatusCode(entity.getHttpStatusCode())
                .language(entity.getLanguage())
                .active(entity.getActive())
                .createdBy(entity.getCreatedBy())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public ErrorMapping toEntity(ErrorMappingDto dto) {
        DestinationApi destinationApi = destinationApiRepository.findById(dto.getDestinationApiId())
                .orElseThrow(() -> new EntityNotFoundException("DestinationApi not found"));

        return ErrorMapping.builder()
                .id(dto.getId())
                .destinationApi(destinationApi)
                .destinationSystemName(dto.getDestinationSystemName())
                .rawErrorSubstring(dto.getRawErrorSubstring())
                .matchType(dto.getMatchType())
                .mappedErrorCode(dto.getMappedErrorCode())
                .mappedMessage(dto.getMappedMessage())
                .errorCategory(dto.getErrorCategory())
                .httpStatusCode(dto.getHttpStatusCode())
                .language(dto.getLanguage())
                .active(dto.getActive())
                .createdBy(dto.getCreatedBy())
                .createdAt(dto.getCreatedAt())
                .updatedAt(dto.getUpdatedAt())
                .build();
    }
}
