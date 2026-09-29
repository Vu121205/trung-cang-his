package com.trungcang.trung_cang_his.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.*;

@Entity
@Table(name="users")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class User {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false, unique=true, length=100) private String username;
    @JsonIgnore
    @Column(nullable=false, length=255) private String password;
    @Column(nullable=false, length=150) private String fullName;
    @Column(length=20) private String phone;
    @Column(length=150) private String email;
    @Column(unique=true, length=50) private String employeeCode;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="role_id", nullable=false) private Role role;
    @Column(length=150) private String specialization;
    private LocalDate dateOfBirth;
    @Column(length=10) private String gender;
    @Column(length=500) private String address;
    @Column(length=20) private String identityNumber;
    @Column(length=150) private String department;
    @Column(length=150) private String jobTitle;
    @JsonIgnore
    @Column(nullable=false, columnDefinition="boolean default false") private boolean deleted = false;
    @JsonIgnore
    @Column(nullable=false, columnDefinition="bigint default 0") private long securityVersion = 0;
    @Enumerated(EnumType.STRING) @Column(length=20) private Status status = Status.ACTIVE;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    public enum Status { ACTIVE, INACTIVE }
}
