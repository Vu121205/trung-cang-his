package com.trungcang.trung_cang_his.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name="examination_diagnoses", uniqueConstraints=@UniqueConstraint(name="uk_exam_diagnosis", columnNames={"examination_id","diagnosis_id"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class ExaminationDiagnosis {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="examination_id", nullable=false) private Examination examination;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="diagnosis_id",nullable=false) private Diagnosis diagnosis;
    @Enumerated(EnumType.STRING) @Column(length=20) private DiagnosisType diagnosisType = DiagnosisType.PRIMARY;
    @Column(columnDefinition="TEXT") private String note;
    public enum DiagnosisType { PRIMARY, SECONDARY }
}
