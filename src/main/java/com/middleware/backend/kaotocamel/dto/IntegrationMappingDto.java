package com.middleware.backend.kaotocamel.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IntegrationMappingDto {

    private Long id;
    private String apiName;
    private String externalSystem;
    private String datasetId;
    private String dataElementId;
    private String categoryOptionComboId;
    private String attributeOptionComboId;
    private String externalKey;
    private Boolean isActive;
    private String notes;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
}