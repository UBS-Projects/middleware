package com.middleware.backend.spec;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.springframework.data.jpa.domain.Specification;

import com.middleware.backend.errormapping.model.ErrorMapping;

import jakarta.persistence.criteria.Predicate;

public class ErrorMappingSpecification_old {
    public static Specification<ErrorMapping> filter(Map<String, String> filters) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            filters.forEach((key, value) -> {
                if (value != null && !value.isBlank() && hasField(ErrorMapping.class, key)) {
                    predicates.add(cb.equal(root.get(key), parseValue(ErrorMapping.class, key, value)));
                }
            });

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static boolean hasField(Class<?> clazz, String fieldName) {
        return Arrays.stream(clazz.getDeclaredFields()).anyMatch(f -> f.getName().equals(fieldName));
    }

    private static Object parseValue(Class<?> clazz, String fieldName, String value) {
        try {
            Field field = clazz.getDeclaredField(fieldName);
            Class<?> type = field.getType();
            if (type.equals(Integer.class))
                return Integer.valueOf(value);
            if (type.equals(Long.class))
                return Long.valueOf(value);
            if (type.equals(Boolean.class))
                return Boolean.valueOf(value);
            if (type.equals(String.class))
                return value;
            // Add LocalDateTime parsing if needed
        } catch (NoSuchFieldException | NumberFormatException | SecurityException e) {
            return value;
        }
        return value;
    }
}
