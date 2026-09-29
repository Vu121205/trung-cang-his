package com.trungcang.trung_cang_his.service;

import com.trungcang.trung_cang_his.domain.Invoice;

import java.util.List;

public interface InvoiceService {
    List<Invoice> getAll();
    Invoice getById(Long id);
    Invoice create(Invoice invoice);
    Invoice update(Long id, Invoice invoice);
    void delete(Long id);
}
