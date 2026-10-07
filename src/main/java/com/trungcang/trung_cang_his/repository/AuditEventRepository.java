package com.trungcang.trung_cang_his.repository;

import com.trungcang.trung_cang_his.domain.AuditEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditEventRepository extends JpaRepository<AuditEvent, Long> {
}
