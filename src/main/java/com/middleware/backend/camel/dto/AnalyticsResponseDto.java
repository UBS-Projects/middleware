package com.middleware.backend.camel.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AnalyticsResponseDto {

    @JsonProperty("headers")
    private List<HeaderDto> headers;

    @JsonProperty("metaData")
    private MetaDataDto metaData;

    @JsonProperty("rows")
    private List<List<Object>> rows;

    @JsonProperty("width")
    private Integer width;

    @JsonProperty("height")
    private Integer height;

     public List<String> getHeaderNames() {
        if (headers == null) return null;
        return headers.stream()
                .map(HeaderDto::getName)
                .toList();
    }

     public int getHeaderIndex(String headerName) {
        if (headers == null) return -1;
        for (int i = 0; i < headers.size(); i++) {
            if (headerName.equals(headers.get(i).getName())) {
                return i;
            }
        }
        return -1;
    }
}