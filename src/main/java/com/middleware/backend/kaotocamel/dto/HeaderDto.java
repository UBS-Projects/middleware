package com.middleware.backend.kaotocamel.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class HeaderDto {

    @JsonProperty("name")
    private String name;

    @JsonProperty("column")
    private String column;

    @JsonProperty("type")
    private String type;

    @JsonProperty("hidden")
    private Boolean hidden;

    @JsonProperty("meta")
    private Boolean meta;
}