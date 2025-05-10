//package com.middleware.backend.example;
//
//import jakarta.persistence.*;
//
//@Entity
//public class TransformationMapping {
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;
//
//    private String targetField;
//
//    private String spelExpression;
//
//    @ManyToOne
//    @JoinColumn(name = "api_id")
//    private ExternalApi api;
//
//    // Getters and setters
//
//    public Long getId() {
//        return id;
//    }
//
//    public void setId(Long id) {
//        this.id = id;
//    }
//
//    public String getTargetField() {
//        return targetField;
//    }
//
//    public void setTargetField(String targetField) {
//        this.targetField = targetField;
//    }
//
//    public String getSpelExpression() {
//        return spelExpression;
//    }
//
//    public void setSpelExpression(String spelExpression) {
//        this.spelExpression = spelExpression;
//    }
//
//    public ExternalApi getApi() {
//        return api;
//    }
//
//    public void setApi(ExternalApi api) {
//        this.api = api;
//    }
//}
//
