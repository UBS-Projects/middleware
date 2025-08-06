package com.middleware.backend.orchestration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class StepProcessorRegistry {

    private final Map<String, StepProcessor> processorMap;

    @Autowired
    public StepProcessorRegistry(List<StepProcessor> processors) {
        processorMap = new HashMap<>();
        for (StepProcessor processor : processors) {
            processorMap.put(processor.getClass().getAnnotation(Component.class).value(), processor);
        }
    }

    public StepProcessor getProcessor(String stepType) {
        return processorMap.get(stepType);
    }
}
