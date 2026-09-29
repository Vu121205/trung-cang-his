package com.trungcang.trung_cang_his.domain;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.*;

@Entity
@Table(name="invoices")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Invoice {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false, unique=true, length=50) private String invoiceCode;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="visit_id", nullable=false) private Visit visit;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="patient_id", nullable=false) private Patient patient;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="cashier_id") private User cashier;
    private LocalDateTime invoiceDate;
    @Column(nullable=false, precision=15, scale=2) private BigDecimal subtotal = BigDecimal.ZERO;
    @Column(precision=15, scale=2) private BigDecimal discountAmount = BigDecimal.ZERO;
    @Column(precision=15, scale=2) private BigDecimal insuranceAmount = BigDecimal.ZERO;
    @Column(nullable=false, precision=15, scale=2) private BigDecimal totalAmount = BigDecimal.ZERO;
    @Column(precision=15, scale=2) private BigDecimal paidAmount = BigDecimal.ZERO;
    @Column(precision=15, scale=2) private BigDecimal debtAmount = BigDecimal.ZERO;
    @Enumerated(EnumType.STRING) @Column(length=20) private PaymentMethod paymentMethod = PaymentMethod.CASH;
    @Enumerated(EnumType.STRING) @Column(length=20) private Status status = Status.UNPAID;
    @Column(columnDefinition="TEXT") private String note;
    private LocalDateTime createdAt;
    public enum PaymentMethod { CASH, BANK_TRANSFER, VNPAY, MOMO, OTHER }
    public enum Status { UNPAID, PARTIAL, PAID, CANCELLED }
}
