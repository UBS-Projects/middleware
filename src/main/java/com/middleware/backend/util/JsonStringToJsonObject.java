package com.middleware.backend.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonStringToJsonObject {
    public  String convert(String jsonString) throws Exception {
        // String containing JSON schema
        // jsonString = "{ \"$schema\": \"http://json-schema.org/draft-07/schema#\", \"type\": \"object\", \"required\": [\"stepName\", \"stepType\", \"sequence\", \"workflowConfigId\", \"destinationApiId\"], \"properties\": { \"stepName\": { \"type\": \"string\", \"minLength\": 1 }, \"stepType\": { \"type\": \"string\", \"enum\": [\"VALIDATION\", \"TRANSFORMATION\", \"INTEGRATION\", \"MAPPING\", \"OTHER\"] }, \"sequence\": { \"type\": \"integer\", \"minimum\": 1 }, \"forkGroup\": { \"type\": \"string\" }, \"config\": { \"type\": \"string\" }, \"workflowConfigId\": { \"type\": \"integer\", \"minimum\": 1 }, \"destinationApiId\": { \"type\": \"integer\", \"minimum\": 1 } }, \"additionalProperties\": false }";
        
        // Convert the string back to a JSON object
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(jsonString);
        
        // Output the formatted JSON
        String formattedJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonNode);
        System.out.println(formattedJson);
        return formattedJson;

    }
}
