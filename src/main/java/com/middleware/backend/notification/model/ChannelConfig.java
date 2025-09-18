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
@Table(name = "notification_channels")
public class ChannelConfig {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private ChannelType type;   // EMAIL or SMS

    private String name;        // e.g., "Default Email"
    @Column(columnDefinition = "TEXT")
    private String config;      // JSON with credentials & api_url

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
