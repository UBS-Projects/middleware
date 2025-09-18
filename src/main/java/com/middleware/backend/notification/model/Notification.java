//package com.middleware.backend.notification.model;
//
//import com.middleware.backend.notification.enums.NotificationStatus;
//import jakarta.persistence.*;
//import lombok.*;
//
//import java.sql.Timestamp;
//import java.util.List;
//
//@Data
//@AllArgsConstructor
//@NoArgsConstructor
//@Builder
//@Entity
//@Table(name = "notifications")
//public class Notification {
//
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;
//
//    private String subject;
//
//    @Column(columnDefinition = "TEXT")
//    private String body;
//
//    @ManyToOne(fetch = FetchType.LAZY)
//    private NotificationTemplate template;
//
//    @ManyToOne(fetch = FetchType.LAZY)
//    private ChannelConfig channel;
//
//    @ManyToMany(fetch = FetchType.LAZY)
//    @JoinTable(
//            name = "notification_receivers",
//            joinColumns = @JoinColumn(name = "notification_id"),
//            inverseJoinColumns = @JoinColumn(name = "receiver_id")
//    )
//    private List<Receiver> receivers;
//
//    @Enumerated(EnumType.STRING)
//    private NotificationStatus status;  // PENDING, SENT, FAILED
//
//    private Timestamp scheduledAt; // optional
//
//    private Timestamp sentAt;
//
//    @Column(name="created_by")
//    private String createdBy;
//    @Column(name="created_at")
//    private Timestamp createdAt;
//    @Column(name="updated_by")
//    private String updatedBy;
//    @Column(name="updated_at")
//    private Timestamp updatedAt;
//}
