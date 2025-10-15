package com.middleware.backend.kaotocamel.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

@Data
@NoArgsConstructor
 public class AttributeDto {
    private String name;
    private Object value;

    @JsonInclude(JsonInclude.Include.NON_NULL)  // لا تعرض هذا الحقل إذا كان null
    private String attName;

    // Constructor بدون attName (للحالات العادية)
    public AttributeDto(String name, Object value) {
        this.name = name;
        this.value = value;
        this.attName = null;
    }

    // Constructor مع attName (للحالات مع attribute)
    public AttributeDto(String name, Object value, String attName) {
        this.name = name;
        this.value = value;
        this.attName = attName;
    }
}