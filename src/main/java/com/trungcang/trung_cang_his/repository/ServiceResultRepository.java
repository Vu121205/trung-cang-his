package com.trungcang.trung_cang_his.repository;

import com.trungcang.trung_cang_his.domain.ServiceResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ServiceResultRepository extends JpaRepository<ServiceResult, Long> {
}
