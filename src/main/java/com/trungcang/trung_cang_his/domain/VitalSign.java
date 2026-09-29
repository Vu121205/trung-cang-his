package com.trungcang.trung_cang_his.domain;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.*;

@Entity
@Table(name="vital_signs")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class VitalSign {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @OneToOne(fetch=FetchType.LAZY) @JoinColumn(name="visit_id", nullable=false, unique=true) private Visit visit;
    private BigDecimal bloodPressureSystolic;
    private BigDecimal bloodPressureDiastolic;
    private Integer pulse;
    private BigDecimal temperature;
    private Integer respiratoryRate;
    private BigDecimal weight;
    private BigDecimal height;
    private BigDecimal spo2;
    private BigDecimal bmi;
    @Column(columnDefinition="TEXT") private String note;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="measured_by") private User measuredBy;
    private LocalDateTime measuredAt;
}
