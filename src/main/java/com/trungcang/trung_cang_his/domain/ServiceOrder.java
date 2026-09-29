package com.trungcang.trung_cang_his.domain;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.*;

@Entity
@Table(name="service_orders")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class ServiceOrder {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false, unique=true, length=50) private String orderCode;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="visit_id", nullable=false) private Visit visit;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="examination_id") private Examination examination;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="service_id", nullable=false) private MedicalService service;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="ordered_by", nullable=false) private User orderedBy;
    private Integer quantity = 1;
    @Column(nullable=false, precision=15, scale=2) private BigDecimal unitPrice = BigDecimal.ZERO;
    @Column(nullable=false, precision=15, scale=2) private BigDecimal totalPrice = BigDecimal.ZERO;
    @Enumerated(EnumType.STRING) @Column(length=20) private Status status = Status.ORDERED;
    private LocalDateTime orderedAt;
    public enum Status { ORDERED, WAITING, PERFORMING, COMPLETED, CANCELLED }
}
