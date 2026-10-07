package com.trungcang.trung_cang_his.repository;

import com.trungcang.trung_cang_his.domain.Prescription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PrescriptionRepository extends JpaRepository<Prescription, Long> {
	java.util.List<Prescription> findAllByStatusOrderByPrescriptionDateAsc(Prescription.Status status);
	java.util.List<Prescription> findAllByVisit_Id(Long visitId);
	java.util.Optional<Prescription> findByExamination_Id(Long examinationId);
}
