package com.trungcang.trung_cang_his.domain;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.*;

@Entity
@Table(name="patients")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Patient {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false, unique=true, length=30) private String patientCode;
    @Column(nullable=false, length=150) private String fullName;
    private LocalDate dateOfBirth;
    @Enumerated(EnumType.STRING) @Column(length=10) private Gender gender;
    @Column(length=20) private String phone;
    @Column(length=20) private String identityNumber;
    @Column(length=30) private String healthInsuranceNumber;
    @Column(length=255) private String address;
    @Column(length=100) private String occupation;
    @Column(length=150) private String emergencyContactName;
    @Column(length=20) private String emergencyContactPhone;
    @Column(length=10) private String bloodType;
    @Column(columnDefinition="TEXT") private String allergyNote;
    @Column(columnDefinition="TEXT") private String medicalHistory;
    @Enumerated(EnumType.STRING) @Column(length=20) private Status status = Status.ACTIVE;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    public enum Gender { MALE, FEMALE, OTHER }
    public enum Status { ACTIVE, INACTIVE }
}
