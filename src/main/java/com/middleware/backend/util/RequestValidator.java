package com.middleware.backend.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

@Component
public class RequestValidator {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final JsonSchemaFactory schemaFactory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);

    public boolean validateJson(String schemaStr, JsonNode data) {
        try {
            JsonSchema schema = schemaFactory.getSchema(schemaStr);
            Set<ValidationMessage> errors = schema.validate(data);
            return errors.isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    public boolean validateHeaders(String headerTemplateJson, Map<String, String> actualHeaders) {
        try {
            JsonNode requiredHeaders = objectMapper.readTree(headerTemplateJson);
            for (JsonNode key : requiredHeaders) {
                if (!actualHeaders.containsKey(key.asText())) {
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
