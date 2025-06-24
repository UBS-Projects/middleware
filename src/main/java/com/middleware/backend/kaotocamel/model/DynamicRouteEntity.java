package com.middleware.backend.kaotocamel.model;

//package com.example.kaotocamel.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "dynamic_routes")
@Data
@NoArgsConstructor
public class DynamicRouteEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String routeId; // The unique route identifier (from YAML)

    private int version; // Version number of the route

    // @Lob
    // private String yamlContent; // YAML source of the route
    @Lob
    @Column(name = "yaml_content", columnDefinition = "CLOB")
    private String yamlContent;

    private boolean active; // Is this version currently active?

    private LocalDateTime createdAt;

    private String comment; // Admin comment for this version
}
