package com.trungcang.trung_cang_his.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.*;

@Entity
@Table(name="examinations")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Examination {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @OneToOne(fetch=FetchType.LAZY) @JoinColumn(name="visit_id", nullable=false, unique=true) private Visit visit;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="doctor_id", nullable=false) private User doctor;
    @Column(columnDefinition="TEXT") private String chiefComplaint;
    @Column(columnDefinition="TEXT") private String symptoms;
    @Column(columnDefinition="TEXT") private String medicalHistory;
    @Column(columnDefinition="TEXT") private String physicalExamination;
    @Column(columnDefinition="TEXT") private String clinicalNote;
    @Column(columnDefinition="TEXT") private String advice;
    @Column(columnDefinition="TEXT") private String examinationResult;
    @Enumerated(EnumType.STRING) @Column(length=20) private Status status = Status.DRAFT;
    private LocalDateTime examinedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    public enum Status { DRAFT, COMPLETED }
}
