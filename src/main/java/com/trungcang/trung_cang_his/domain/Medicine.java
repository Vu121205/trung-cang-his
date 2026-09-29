package com.trungcang.trung_cang_his.domain;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.*;

@Entity
@Table(name="medicines")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Medicine {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false, unique=true, length=50) private String code;
    @Column(nullable=false, length=255) private String name;
    @Column(length=255) private String activeIngredient;
    @Column(length=100) private String strength;
    @Column(length=100) private String dosageForm;
    @Column(nullable=false, length=50) private String unit;
    @Column(length=255) private String manufacturer;
    @Column(length=100) private String registrationNumber;
    @Column(nullable=false, precision=15, scale=2) private BigDecimal price = BigDecimal.ZERO;
    private Integer minStock = 0;
    @Enumerated(EnumType.STRING) @Column(length=20) private Status status = Status.ACTIVE;
    private LocalDateTime createdAt;
    public enum Status { ACTIVE, INACTIVE }
}
