package com.trungcang.trung_cang_his.domain;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.*;

@Entity
@Table(name="services")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class MedicalService {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false, unique=true, length=50) private String code;
    @Column(nullable=false, length=255) private String name;
    @Enumerated(EnumType.STRING) @Column(nullable=false, length=20) private ServiceType type;
    @Column(length=50) private String unit = "Lần";
    @Column(nullable=false, precision=15, scale=2) private BigDecimal price = BigDecimal.ZERO;
    @Column(precision=15, scale=2) private BigDecimal insurancePrice;
    @Column(columnDefinition="TEXT") private String description;
    @Enumerated(EnumType.STRING) @Column(length=20) private Status status = Status.ACTIVE;
    private LocalDateTime createdAt;
    public enum ServiceType { EXAM, LAB, IMAGE, PROCEDURE, OTHER }
    public enum Status { ACTIVE, INACTIVE }
}
