package com.middleware.backend.camel.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * UPDATED DTO for middleware response row
 * Now groups by OU only - ouDetails appear once, periods are nested inside
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MiddlewareRowDto {

    /**
     * Organization Unit ID
     */
    private String ou;

    /**
     * Organization Unit display name
     */
    private String ouName;

    /**
     * Full organization unit metadata (code, parent, translations, etc.)
     * This appears ONCE per OU (not repeated for each period)
     */
    private Map<String, Object> ouDetails;

    /**
     * NEW: List of periods with their data
     * Each period contains its attributes
     */
    private List<PeriodDataDto> periods;


}