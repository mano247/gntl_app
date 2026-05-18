package com.gentlemanstore.loyalty.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "points_rules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PointsRule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Integer points;

    @Column(nullable = false)
    private boolean deleted = false;
}
