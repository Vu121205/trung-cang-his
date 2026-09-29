package com.trungcang.trung_cang_his.domain;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.*;

@Entity
@Table(name="medicine_batches", uniqueConstraints=@UniqueConstraint(name="uk_medicine_batch", columnNames={"medicine_id","batch_number"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class MedicineBatch {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="medicine_id", nullable=false) private Medicine medicine;
    @Column(nullable=false, length=100) private String batchNumber;
    private LocalDate manufactureDate;
    private LocalDate expiryDate;
    @Column(nullable=false) private Integer quantity = 0;
    @Column(nullable=false, precision=15, scale=2) private BigDecimal unitPrice = BigDecimal.ZERO;
    private LocalDateTime createdAt;
}
