package com.trungcang.trung_cang_his.domain;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.*;

@Entity
@Table(name="prescriptions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Prescription {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false, unique=true, length=50) private String prescriptionCode;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="visit_id", nullable=false) private Visit visit;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="examination_id") private Examination examination;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="doctor_id", nullable=false) private User doctor;
    private LocalDateTime prescriptionDate;
    @Column(columnDefinition="TEXT") private String note;
    @Enumerated(EnumType.STRING) @Column(length=20) private Status status = Status.DRAFT;
    @Column(precision=15, scale=2) private BigDecimal totalAmount = BigDecimal.ZERO;
    public enum Status { DRAFT, PRESCRIBED, DISPENSED, CANCELLED }
}
