package com.gentlemanstore.notification.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "notifications_template")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationTemplate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String titleTemplate;

    @Column(nullable = false)
    private String messageTemplate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType notificationType;

    @Column(nullable = false)
    private boolean deleted = false;
}
