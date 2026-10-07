package com.trungcang.trung_cang_his.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/** Immutable business audit entry. It deliberately stores no clinical payload. */
@Entity
@Table(name = "audit_events")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class AuditEvent {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 80)
    private String eventType;

    @Column(nullable = false, length = 80)
    private String entityType;

    @Column(length = 80)
    private String entityId;

    @Column(nullable = false, length = 100)
    private String actorUsername;

    @Column(nullable = false)
    private LocalDateTime occurredAt;

    @Column(columnDefinition = "TEXT")
    private String detail;
}
