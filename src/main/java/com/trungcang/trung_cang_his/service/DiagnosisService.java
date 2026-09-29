package com.trungcang.trung_cang_his.service;

import com.trungcang.trung_cang_his.domain.Diagnosis;

import java.util.List;

public interface DiagnosisService {
    List<Diagnosis> getAll();
    Diagnosis getById(Long id);
    Diagnosis create(Diagnosis diagnosis);
    Diagnosis update(Long id, Diagnosis diagnosis);
    void delete(Long id);
}
