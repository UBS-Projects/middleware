package com.middleware.backend.service;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

import com.middleware.backend.model.DestinationApi;
import com.middleware.backend.model.ErrorMapping;
import com.middleware.backend.repository.ErrorMappingRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ErrorMappingService {

    private final ErrorMappingRepository errorMappingRepository;

    public Map<String, Object> mapError(DestinationApi destinationApi, String rawError) {
        log.info("Mapping error for DestinationApi: {}, RawError: {}", destinationApi.getName(), rawError);

        if (rawError == null || rawError.isBlank()) {
            log.warn("Empty raw error received for mapping.");
            return Map.of(
                    "mappedErrorCode", "UNKNOWN_ERROR",
                    "mappedMessage", "An unexpected error occurred.",
                    "errorCategory", "GENERAL",
                    "httpStatusCode", 500
            );
        }

        List<ErrorMapping> mappings = errorMappingRepository.findByDestinationApiIdAndActive(destinationApi.getId(), true);

        for (ErrorMapping mapping : mappings) {
            boolean isMatch = switch (mapping.getMatchType()) {
                case "EQUALS" -> rawError.equals(mapping.getRawErrorSubstring());
                case "REGEX" -> Pattern.compile(mapping.getRawErrorSubstring()).matcher(rawError).find();
                default -> rawError.contains(mapping.getRawErrorSubstring());
            };

            if (isMatch) {
                log.info("Matched ErrorMapping: {}", mapping);
                return Map.of(
                        "mappedErrorCode", mapping.getMappedErrorCode(),
                        "mappedMessage", mapping.getMappedMessage(),
                        "errorCategory", mapping.getErrorCategory() != null ? mapping.getErrorCategory() : "GENERAL",
                        "httpStatusCode", mapping.getHttpStatusCode() != null ? mapping.getHttpStatusCode() : 500
                );
            }
        }

        log.warn("No matching error mapping found. Returning default mapping.");
        return Map.of(
                "mappedErrorCode", "UNMAPPED_ERROR",
                "mappedMessage", "An error occurred, but no specific mapping was found.",
                "errorCategory", "GENERAL",
                "httpStatusCode", 500
        );
    }



    
}
