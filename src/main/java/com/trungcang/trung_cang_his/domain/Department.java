package com.trungcang.trung_cang_his.domain;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.*;

@Entity
@Table(name="departments")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Department {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false, unique=true, length=50) private String code;
    @Column(nullable=false, length=150) private String name;
    @Column(length=255) private String description;
    @Enumerated(EnumType.STRING) @Column(length=20) private Status status = Status.ACTIVE;
    private LocalDateTime createdAt;
    public enum Status { ACTIVE, INACTIVE }
}
