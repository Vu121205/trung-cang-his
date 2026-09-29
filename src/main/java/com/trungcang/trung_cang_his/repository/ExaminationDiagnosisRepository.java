package com.trungcang.trung_cang_his.repository;

import com.trungcang.trung_cang_his.domain.ExaminationDiagnosis;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ExaminationDiagnosisRepository extends JpaRepository<ExaminationDiagnosis, Long> {
    List<ExaminationDiagnosis> findByExamination_Id(Long id);
}
