package com.middleware.backend.util;

import com.middleware.backend.model.ErrorMapping;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Slf4j
@Component
public class ErrorMatcher {

    /**
     * Matches the raw error message with the given list of ErrorMappings.
     * Supports CONTAINS, EQUALS, REGEX match types.
     *
     * @param rawError The raw error message to match.
     * @param mappings The list of ErrorMappings to evaluate.
     * @return Optional of matched ErrorMapping if found.
     */
    public Optional<ErrorMapping> matchError(String rawError, List<ErrorMapping> mappings) {
        log.debug("Matching rawError [{}] against {} error mappings", rawError, mappings.size());

        for (ErrorMapping mapping : mappings) {
            String pattern = mapping.getRawErrorSubstring();
            String matchType = mapping.getMatchType();

            if (pattern == null || matchType == null || !mapping.getActive()) {
                continue;
            }

            log.debug("Evaluating ErrorMapping: [id={}, matchType={}, pattern={}]", mapping.getId(), matchType, pattern);

            boolean matched = false;
            switch (matchType.toUpperCase()) {
                case "CONTAINS":
                    matched = rawError.contains(pattern);
                    break;

                case "EQUALS":
                    matched = rawError.equals(pattern);
                    break;

                case "REGEX":
                    matched = rawError.matches(pattern);
                    break;

                default:
                    log.warn("Unknown matchType [{}] in ErrorMapping id={}", matchType, mapping.getId());
                    break;
            }

            if (matched) {
                log.info("Matched ErrorMapping: id={}, mappedErrorCode={}, message={}",
                        mapping.getId(), mapping.getMappedErrorCode(), mapping.getMappedMessage());
                return Optional.of(mapping);
            }
        }

        log.info("No matching ErrorMapping found for rawError [{}]", rawError);
        return Optional.empty();
    }
}
