//package com.middleware.backend.kaotocamel.model;
//
//import com.fasterxml.jackson.annotation.JsonFormat;
//import jakarta.persistence.*;
//import java.time.LocalDateTime;
//
//@Entity
//@Table(name = "integration_mapping",
//        uniqueConstraints = {
//                @UniqueConstraint(name = "uk_api_external_key_active",
//                        columnNames = {"api_name", "external_key", "is_active"}),
//                @UniqueConstraint(name = "uk_api_dhis2_source_active",
//                        columnNames = {"api_name", "dataset_id", "data_element_id",
//                                "category_option_combo_id", "attribute_option_combo_id",
//                                "external_key", "is_active"})
//        })
//public class IntegrationMapping {
//
//    @Id
//    @Column(name = "id")
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;
//
//    @Column(name = "api_name", nullable = false, length = 100)
//    private String apiName;
//
//    @Column(name = "external_system", nullable = false, length = 50)
//    private String externalSystem;
//
//    @Column(name = "dataset_id", nullable = false, length = 500)
//    private String datasetId;
//
//    @Column(name = "data_element_id", nullable = false, length = 50)
//    private String dataElementId;
//
//    @Column(name = "category_option_combo_id", nullable = false, length = 50)
//    private String categoryOptionComboId;
//
//    @Column(name = "attribute_option_combo_id", length = 50)
//    private String attributeOptionComboId;
//
//    @Column(name = "external_key", nullable = false, length = 100)
//    private String externalKey;
//
//    @Column(name = "is_active")
//    private Boolean isActive;
//
//    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
//    @Column(name = "created_at")
//    private LocalDateTime createdAt;
//
//    @Column(name = "notes", columnDefinition = "text")
//    private String notes;
//
//    // Constructors
//    public IntegrationMapping() {
//    }
//
//    public IntegrationMapping(String apiName, String externalSystem,
//                              String datasetId, String dataElementId, String categoryOptionComboId,
//                              String externalKey) {
//        this.apiName = apiName;
//        this.externalSystem = externalSystem;
//        this.datasetId = datasetId;
//        this.dataElementId = dataElementId;
//        this.categoryOptionComboId = categoryOptionComboId;
//        this.externalKey = externalKey;
//    }
//
//    @PrePersist
//    protected void onCreate() {
//        if (this.createdAt == null) {
//            this.createdAt = LocalDateTime.now();
//        }
//        if (this.isActive == null) {
//            this.isActive = true;
//        }
//    }
//
//    // Getters and Setters
//    public Long getId() {
//        return id;
//    }
//
//    public void setId(Long id) {
//        this.id = id;
//    }
//
//    public String getApiName() {
//        return apiName;
//    }
//
//    public void setApiName(String apiName) {
//        this.apiName = apiName;
//    }
//
//    public String getExternalSystem() {
//        return externalSystem;
//    }
//
//    public void setExternalSystem(String externalSystem) {
//        this.externalSystem = externalSystem;
//    }
//
//    public String getDatasetId() {
//        return datasetId;
//    }
//
//    public void setDatasetId(String datasetId) {
//        this.datasetId = datasetId;
//    }
//
//    public String getDataElementId() {
//        return dataElementId;
//    }
//
//    public void setDataElementId(String dataElementId) {
//        this.dataElementId = dataElementId;
//    }
//
//    public String getCategoryOptionComboId() {
//        return categoryOptionComboId;
//    }
//
//    public void setCategoryOptionComboId(String categoryOptionComboId) {
//        this.categoryOptionComboId = categoryOptionComboId;
//    }
//
//    public String getAttributeOptionComboId() {
//        return attributeOptionComboId;
//    }
//
//    public void setAttributeOptionComboId(String attributeOptionComboId) {
//        this.attributeOptionComboId = attributeOptionComboId;
//    }
//
//    public String getExternalKey() {
//        return externalKey;
//    }
//
//    public void setExternalKey(String externalKey) {
//        this.externalKey = externalKey;
//    }
//
//    public Boolean getIsActive() {
//        return isActive;
//    }
//
//    public void setIsActive(Boolean isActive) {
//        this.isActive = isActive;
//    }
//
//    public LocalDateTime getCreatedAt() {
//        return createdAt;
//    }
//
//    public void setCreatedAt(LocalDateTime createdAt) {
//        this.createdAt = createdAt;
//    }
//
//    public String getNotes() {
//        return notes;
//    }
//
//    public void setNotes(String notes) {
//        this.notes = notes;
//    }
//
//    @Override
//    public String toString() {
//        return "IntegrationMapping{" +
//                "id=" + id +
//                ", apiName='" + apiName + '\'' +
//                ", externalSystem='" + externalSystem + '\'' +
//                ", datasetId='" + datasetId + '\'' +
//                ", dataElementId='" + dataElementId + '\'' +
//                ", categoryOptionComboId='" + categoryOptionComboId + '\'' +
//                ", attributeOptionComboId='" + attributeOptionComboId + '\'' +
//                ", externalKey='" + externalKey + '\'' +
//                ", isActive=" + isActive +
//                '}';
//    }
//}