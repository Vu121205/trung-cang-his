package com.trungcang.trung_cang_his.service.impl;

import com.trungcang.trung_cang_his.domain.Invoice;
import com.trungcang.trung_cang_his.repository.InvoiceRepository;
import com.trungcang.trung_cang_his.service.InvoiceService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class InvoiceServiceImpl implements InvoiceService {

    private final InvoiceRepository invoiceRepository;

    public InvoiceServiceImpl(InvoiceRepository invoiceRepository) {
        this.invoiceRepository = invoiceRepository;
    }

    @Override
    public List<Invoice> getAll() {
        return invoiceRepository.findAll();
    }

    @Override
    public Invoice getById(Long id) {
        return invoiceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Invoice not found with id: " + id));
    }

    @Override
    public Invoice create(Invoice invoice) {
        return invoiceRepository.save(invoice);
    }

    @Override
    public Invoice update(Long id, Invoice invoice) {
        Invoice existing = getById(id);
        existing.setInvoiceCode(invoice.getInvoiceCode());
        existing.setTotalAmount(invoice.getTotalAmount());
        existing.setStatus(invoice.getStatus());
        return invoiceRepository.save(existing);
    }

    @Override
    public void delete(Long id) {
        invoiceRepository.deleteById(id);
    }
}
