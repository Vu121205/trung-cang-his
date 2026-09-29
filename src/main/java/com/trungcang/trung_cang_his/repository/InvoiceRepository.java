package com.trungcang.trung_cang_his.repository;

import com.trungcang.trung_cang_his.domain.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
	java.util.Optional<Invoice> findFirstByVisit_IdOrderByIdDesc(Long visitId);
}
