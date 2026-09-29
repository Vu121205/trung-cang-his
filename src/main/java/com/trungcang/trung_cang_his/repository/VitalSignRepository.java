package com.trungcang.trung_cang_his.repository;

import com.trungcang.trung_cang_his.domain.VitalSign;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VitalSignRepository extends JpaRepository<VitalSign, Long> {
    Optional<VitalSign> findByVisit_Id(Long visitId);
}