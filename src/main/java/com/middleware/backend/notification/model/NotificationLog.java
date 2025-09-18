package com.middleware.backend.notification.model;

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
@Table(name = "notification_logs")
public class NotificationLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private NotificationGroup group;

    @ManyToOne
    private NotificationTemplate template;

    @ManyToOne
    private ChannelConfig channel;

    private String status; // PENDING, SUCCESS, FAILED

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name="sent_at")
    private Timestamp sentAt;
}