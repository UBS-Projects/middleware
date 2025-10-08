package com.middleware.backend.notification.model;

import com.middleware.backend.notification.enums.ChannelType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "notification_templates")
public class NotificationTemplate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "code", unique = true)
    private String code;


    private String name;
    @Enumerated(EnumType.STRING)
    private ChannelType type;
    private String subject;
    @Column(columnDefinition = "TEXT")
    private String body;

    private boolean active;

    @Column(name="created_by")
    private String createdBy;
    @Column(name = "created_at")
    private Timestamp createdAt;
    @Column(name="updated_by")
    private String updatedBy;
    @Column(name = "updated_at")
    private Timestamp updatedAt;

}