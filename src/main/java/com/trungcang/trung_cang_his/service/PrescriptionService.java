package com.trungcang.trung_cang_his.service;

import com.trungcang.trung_cang_his.domain.Prescription;

import java.util.List;

public interface PrescriptionService {
    List<Prescription> getAll();
    Prescription getById(Long id);
    Prescription create(Prescription prescription);
    Prescription update(Long id, Prescription prescription);
    void delete(Long id);
}
