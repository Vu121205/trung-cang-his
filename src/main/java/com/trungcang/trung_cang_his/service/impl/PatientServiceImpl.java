package com.trungcang.trung_cang_his.service.impl;

import com.trungcang.trung_cang_his.domain.Patient;
import com.trungcang.trung_cang_his.repository.PatientRepository;
import com.trungcang.trung_cang_his.service.PatientService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
public class PatientServiceImpl implements PatientService {

    private final PatientRepository patientRepository;

    public PatientServiceImpl(PatientRepository patientRepository) {
        this.patientRepository = patientRepository;
    }

    @Override
    public List<Patient> getAll() {
        return patientRepository.findAll();
    }

    @Override
    public Patient getById(Long id) {
        return patientRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Patient not found with id: " + id));
    }

    @Override
    public Patient create(Patient patient) {
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        patient.setCreatedAt(now);
        patient.setUpdatedAt(now);
        return patientRepository.save(patient);
    }

    @Override
    public Patient update(Long id, Patient patient) {
        Patient existing = getById(id);
        existing.setFullName(patient.getFullName());
        existing.setPhone(patient.getPhone());
        existing.setAddress(patient.getAddress());
        existing.setGender(patient.getGender());
        existing.setDateOfBirth(patient.getDateOfBirth());
        existing.setUpdatedAt(LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")));
        return patientRepository.save(existing);
    }

    @Override
    public void delete(Long id) {
        patientRepository.deleteById(id);
    }
}
