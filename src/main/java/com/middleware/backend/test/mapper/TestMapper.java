package com.middleware.backend.test.mapper;

import com.middleware.backend.test.dto.TestDto;
import com.middleware.backend.test.model.Test;

public class TestMapper {
    public static Test mapToEntity(TestDto test){
        return Test.builder()
                .id(test.getId())
                .email(test.getEmail())
                .name(test.getName())
                .build();
    }

    public static TestDto mapToDto(Test test){
        return TestDto.builder()
                .id(test.getId())
                .email(test.getEmail())
                .name(test.getName())
                .build();
    }
}
