package com.gentlemanstore.support.model;

import com.gentlemanstore.order.model.Order;
import com.gentlemanstore.user.model.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "support_tickets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupportTicket {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String subject;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TicketStatus status;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime updatedAt;

    @Column(nullable = false)
    private boolean deleted = false;

    // Staff (employee/admin) je uklonio tiket iz staff liste - tiket nije obrisan,
    // vlasnik (customer) i dalje vidi sopstvenu istoriju.
    @Builder.Default
    @Column(name = "archived_by_staff", nullable = false)
    private boolean archivedByStaff = false;

    // Strukturisana hitnost iz bot upitnika; postojeći tiketi migrirani na MEDIUM.
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TicketUrgency urgency = TicketUrgency.MEDIUM;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Opciona veza ka porudžbini na koju se tiket odnosi. Backend pri
    // kreiranju proverava da porudžbina pripada vlasniku tiketa.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_session_id")
    private ChatSession chatSession;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
