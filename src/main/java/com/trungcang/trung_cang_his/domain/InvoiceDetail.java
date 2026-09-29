package com.trungcang.trung_cang_his.domain;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.*;

@Entity
@Table(name="invoice_details")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class InvoiceDetail {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="invoice_id", nullable=false) private Invoice invoice;
    @Enumerated(EnumType.STRING) @Column(nullable=false, length=20) private ItemType itemType;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="service_id") private MedicalService service;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="medicine_id") private Medicine medicine;
    @Column(nullable=false, length=255) private String description;
    @Column(nullable=false) private Integer quantity = 1;
    @Column(length=50) private String unit;
    @Column(nullable=false, precision=15, scale=2) private BigDecimal unitPrice = BigDecimal.ZERO;
    @Column(precision=15, scale=2) private BigDecimal discountAmount = BigDecimal.ZERO;
    @Column(nullable=false, precision=15, scale=2) private BigDecimal totalPrice = BigDecimal.ZERO;
    public enum ItemType { EXAMINATION, SERVICE, MEDICINE, OTHER }
}
