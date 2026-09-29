package com.trungcang.trung_cang_his.service;

import com.trungcang.trung_cang_his.domain.MedicalService;

import java.util.List;

public interface MedicalServiceService {
    List<MedicalService> getAll();
    MedicalService getById(Long id);
    MedicalService create(MedicalService medicalService);
    MedicalService update(Long id, MedicalService medicalService);
    void delete(Long id);
}
