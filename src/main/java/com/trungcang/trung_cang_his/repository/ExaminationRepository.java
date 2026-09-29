package com.trungcang.trung_cang_his.repository;

import com.trungcang.trung_cang_his.domain.Examination;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ExaminationRepository extends JpaRepository<Examination, Long>,
        org.springframework.data.jpa.repository.JpaSpecificationExecutor<Examination> {
    java.util.Optional<Examination> findByVisit_VisitCode(String visitCode);
}
