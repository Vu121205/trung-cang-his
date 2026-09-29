package com.trungcang.trung_cang_his.domain;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.*;

@Entity
@Table(name="prescription_details")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class PrescriptionDetail {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="prescription_id", nullable=false) private Prescription prescription;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="medicine_id", nullable=false) private Medicine medicine;
    @Column(nullable=false) private Integer quantity;
    @Column(length=50) private String unit;
    @Column(length=255) private String dosage;
    @Column(length=255) private String frequency;
    @Column(length=100) private String duration;
    @Column(length=100) private String route;
    @Column(columnDefinition="TEXT") private String instruction;
    @Column(nullable=false, precision=15, scale=2) private BigDecimal unitPrice = BigDecimal.ZERO;
    @Column(nullable=false, precision=15, scale=2) private BigDecimal totalPrice = BigDecimal.ZERO;
}
