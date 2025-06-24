package com.middleware.backend.util;

import java.util.Set;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class JsonSchemaValidatorUtil {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Validates the input JSON string against the given JSON schema string.
     * 
     * @param schemaStr JSON Schema string (from inputTemplate column).
     * @param inputStr  Actual input JSON string to validate.
     * @return true if valid, false otherwise.
     */
    public boolean validate(String schemaStr, String inputStr) {

        log.debug("Received input: {}", inputStr);
        log.debug("Received schema: {}", schemaStr);
        try {
            JsonNode schemaNode = objectMapper.readTree(schemaStr);
            JsonNode inputNode = objectMapper.readTree(inputStr);

            JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
            JsonSchema schema = factory.getSchema(schemaNode);

            Set<ValidationMessage> errors = schema.validate(inputNode);

            if (errors.isEmpty()) {
                log.info("Validation passedfor input: {}", inputStr);
                return true;
            } else {
                log.warn("Validation failed for input: {} Errors: {}", inputStr, errors);
                return false;
            }
        } catch (Exception e) {
            log.error("Exception during JSON Schema validation: {}", e.getMessage(), e);
            return false;
        }
    }
}
