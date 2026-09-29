package com.trungcang.trung_cang_his.repository;

import com.trungcang.trung_cang_his.domain.Diagnosis;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DiagnosisRepository extends JpaRepository<Diagnosis, Long> {
    java.util.Optional<Diagnosis> findByIcdCode(String icdCode);
}
