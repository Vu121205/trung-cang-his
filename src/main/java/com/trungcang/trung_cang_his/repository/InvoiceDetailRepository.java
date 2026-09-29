package com.trungcang.trung_cang_his.repository;

import com.trungcang.trung_cang_his.domain.InvoiceDetail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InvoiceDetailRepository extends JpaRepository<InvoiceDetail, Long> {
    List<InvoiceDetail> findAllByInvoice_Id(Long invoiceId);
}