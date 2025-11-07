package com.middleware.backend.kaotocamel.dto;

 import lombok.*;

@Data
@NoArgsConstructor
 public class AttributeDto {
    private String name;
    private Object value;

    // Constructor للحالات العادية
    public AttributeDto(String name, Object value) {
        this.name = name;
        this.value = value;
    }
}