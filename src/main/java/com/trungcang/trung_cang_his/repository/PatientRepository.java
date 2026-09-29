package com.trungcang.trung_cang_his.repository;

import com.trungcang.trung_cang_his.domain.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PatientRepository extends JpaRepository<Patient, Long> {
    java.util.Optional<Patient> findByPatientCode(String patientCode);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select p from Patient p where p.patientCode = :code")
    java.util.Optional<Patient> lockByCode(@org.springframework.data.repository.query.Param("code") String code);
}
