package com.middleware.backend.integrated_systems.validation;

import com.middleware.backend.integrated_systems.dto.IntegratedSystemDto;
import com.middleware.backend.integrated_systems.model.SystemType;

/**
 * Defaults and compatibility checks for {@link IntegratedSystemDto}.
 * Existing HTTP/HTTPS payloads without {@code systemType} default to {@link SystemType#HTTP_API}.
 */
public final class IntegratedSystemValidator {

    private IntegratedSystemValidator() {
    }

    /**
     * Fills missing {@code systemType} from protocol (HTTP/HTTPS → HTTP_API) and
     * rejects combinations that are not compatible.
     *
     * @param dto request body to normalize
     * @throws IllegalArgumentException if protocol does not match system type
     */
    public static void applyDefaultsAndValidate(IntegratedSystemDto dto) {
        if (dto == null) {
            throw new IllegalArgumentException("Integrated system payload is required");
        }

        if (dto.getSystemType() == null) {
            dto.setSystemType(SystemType.fromProtocol(dto.getProtocol()));
        }

        if (dto.getProtocol() != null && !dto.getSystemType().supports(dto.getProtocol())) {
            throw new IllegalArgumentException(
                    "Protocol " + dto.getProtocol()
                            + " is not compatible with system type " + dto.getSystemType()
                            + ". Allowed protocols: " + dto.getSystemType().compatibleProtocols());
        }
    }
}
