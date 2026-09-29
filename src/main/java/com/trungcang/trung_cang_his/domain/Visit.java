package com.trungcang.trung_cang_his.domain;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.*;

@Entity
@Table(name="visits")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Visit {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false, unique=true, length=50) private String visitCode;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="patient_id", nullable=false) private Patient patient;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="room_id", nullable=false) private ExaminationRoom room;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="doctor_id") private User doctor;
    private LocalDate visitDate;
    private LocalTime visitTime;
    private Integer queueNumber;
    @Enumerated(EnumType.STRING) @Column(length=20) private VisitType visitType = VisitType.NEW;
    @Enumerated(EnumType.STRING) @Column(length=20) private PaymentType paymentType = PaymentType.SERVICE;
    @Column(columnDefinition="TEXT") private String reason;
    @Enumerated(EnumType.STRING) @Column(length=30) private Status status = Status.WAITING;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="created_by") private User createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    public enum VisitType { NEW, FOLLOW_UP, PERIODIC }
    public enum PaymentType { SERVICE, INSURANCE }
    public enum Status { WAITING, EXAMINING, WAITING_LAB, WAITING_PAYMENT, COMPLETED, CANCELLED }
}
