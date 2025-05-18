package com.middleware.backend.model;

import java.time.LocalDateTime;
import java.util.Map;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "workflow_steps")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkflowStep {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow_config_id", nullable = true)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private WorkflowConfig workflowConfig;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_step_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private WorkflowStep parentStep;

    @Column(name = "step_order", nullable = false)
    private Integer stepOrder;

    @Column(name = "step_name", nullable = false, length = 200)
    private String stepName;

    @Column(name = "step_type", nullable = false, length = 50)
    private String stepType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destination_api_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private DestinationApi destinationApi;


    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "transformation_expression", columnDefinition = "jsonb")
    private Map<String,Object> transformationExpression;

    @Column(name = "delay_seconds")
    private Integer delaySeconds;

    @Column(name = "condition_expression", columnDefinition = "TEXT")
    private String conditionExpression;

    @Column(name = "retry_count")
    private Integer retryCount;

    @Column(name = "retry_delay_seconds")
    private Integer retryDelaySeconds;

    @Column(name = "fork_group_id")
    private Integer forkGroupId;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Column(name = "updated_by", nullable = false)
    private Long updatedBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
