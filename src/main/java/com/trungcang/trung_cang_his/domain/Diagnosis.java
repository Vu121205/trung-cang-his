package com.trungcang.trung_cang_his.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name="diagnoses")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Diagnosis {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false, unique=true, length=20) private String icdCode;
    @Column(nullable=false, length=255) private String name;
    @Column(columnDefinition="TEXT") private String description;
    @Enumerated(EnumType.STRING) @Column(length=20) private Status status = Status.ACTIVE;
    public enum Status { ACTIVE, INACTIVE }
}
