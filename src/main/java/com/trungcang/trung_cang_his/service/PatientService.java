package com.trungcang.trung_cang_his.service;

import com.trungcang.trung_cang_his.domain.Patient;

import java.util.List;

public interface PatientService {
    List<Patient> getAll();
    Patient getById(Long id);
    Patient create(Patient patient);
    Patient update(Long id, Patient patient);
    void delete(Long id);
}
