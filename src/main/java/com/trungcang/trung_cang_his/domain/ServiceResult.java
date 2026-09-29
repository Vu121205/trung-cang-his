package com.trungcang.trung_cang_his.domain;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.*;

@Entity
@Table(name="service_results")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class ServiceResult {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @OneToOne(fetch=FetchType.LAZY) @JoinColumn(name="service_order_id", nullable=false, unique=true) private ServiceOrder serviceOrder;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="technician_id") private User technician;
    @Column(columnDefinition="TEXT") private String resultDescription;
    @Column(columnDefinition="TEXT") private String conclusion;
    @Column(length=500) private String attachmentUrl;
    private LocalDateTime performedAt;
    @Enumerated(EnumType.STRING) @Column(length=20) private Status status = Status.DRAFT;
    private LocalDateTime createdAt;
    public enum Status { DRAFT, COMPLETED, APPROVED }
}
