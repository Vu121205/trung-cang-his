package com.trungcang.trung_cang_his.repository;

import com.trungcang.trung_cang_his.domain.MedicalService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MedicalServiceRepository extends JpaRepository<MedicalService, Long> {
	java.util.Optional<MedicalService> findByCodeAndStatus(String code, MedicalService.Status status);
}
