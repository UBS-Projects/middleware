package com.middleware.backend.notification.model;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "notification_groups")
public class NotificationGroup {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;

    private String description;

    @ManyToMany(fetch = FetchType.EAGER)
    private List<Receiver> receivers;

    @Column(name="created_by")
    private String createdBy;
    @Column(name = "created_at")
    private Timestamp createdAt;
    @Column(name="updated_by")
    private String updatedBy;
    @Column(name = "updated_at")
    private Timestamp updatedAt;
}
