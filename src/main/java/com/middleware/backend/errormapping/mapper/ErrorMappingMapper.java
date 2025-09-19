package com.middleware.backend.errormapping.mapper;

 import com.middleware.backend.errormapping.dto.ErrorMappingDto;
 import com.middleware.backend.errormapping.model.ErrorMapping;
 import org.springframework.stereotype.Component;

@Component
public class ErrorMappingMapper {

    public ErrorMappingDto toDto(ErrorMapping entity) {
        return ErrorMappingDto.builder()
                .id(entity.getId())
                .routeId(entity.getRouteId())
                .routePath(entity.getRoutePath())
                .sourceSystemId(entity.getSourceSystem() != null ? entity.getSourceSystem().getId() : null)
                .sourceSystemName(entity.getSourceSystem() != null ? entity.getSourceSystem().getName() : null)
                .rawErrorSubstring(entity.getRawErrorSubstring())
                .matchType(entity.getMatchType())
                .mappedErrorCode(entity.getMappedErrorCode())
                .mappedMessage(entity.getMappedMessage())
                .errorCategoryId(entity.getErrorCategory() != null ? entity.getErrorCategory().getId() : null)
                .errorCategoryName(entity.getErrorCategory() != null ? entity.getErrorCategory().getName() : null)
                .httpStatusCode(entity.getHttpStatusCode())
                .language(entity.getLanguage())
                .active(entity.getActive())
                .createdBy(entity.getCreatedBy())
                .updatedBy(entity.getUpdatedBy())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
    public ErrorMapping toEntity(ErrorMappingDto dto) {
        return ErrorMapping.builder()
                .id(dto.getId())
                .routeId(trimOrNull(dto.getRouteId()))
                .routePath(trimOrNull(dto.getRoutePath()))
                .rawErrorSubstring(trimOrNull(dto.getRawErrorSubstring().replaceAll("\\s+", " ")))
                .matchType(dto.getMatchType())
                .mappedErrorCode(trimOrNull(dto.getMappedErrorCode()))
                .mappedMessage(trimOrNull(dto.getMappedMessage()))
                .httpStatusCode(dto.getHttpStatusCode())
                .language(trimOrNull(dto.getLanguage()))
                .active(dto.getActive())
                .createdBy(dto.getCreatedBy())
                .updatedBy(dto.getUpdatedBy())
                .createdAt(dto.getCreatedAt())
                .updatedAt(dto.getUpdatedAt())
                .build();
    }
    private String trimOrNull(String input) {
        return input != null ? input.trim() : null;
    }

}