package com.trungcang.trung_cang_his.repository;

import com.trungcang.trung_cang_his.domain.PrescriptionDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PrescriptionDetailRepository extends JpaRepository<PrescriptionDetail, Long> {
    List<PrescriptionDetail> findAllByPrescription_Visit_Id(Long visitId);
    List<PrescriptionDetail> findByPrescription_Examination_Id(Long id);
}
