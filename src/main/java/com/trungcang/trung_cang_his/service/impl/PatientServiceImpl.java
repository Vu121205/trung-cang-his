package com.trungcang.trung_cang_his.service.impl;

import com.trungcang.trung_cang_his.domain.Patient;
import com.trungcang.trung_cang_his.repository.PatientRepository;
import com.trungcang.trung_cang_his.service.PatientService;
import org.springframework.stereotype.Service;

import java.util.Comparator;
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
        if (patient == null) {
            throw new IllegalArgumentException("Patient is required.");
        }
        if (patient.getPatientCode() == null || patient.getPatientCode().isBlank()) {
            patient.setPatientCode(generateNextPatientCode());
        }
        patient.setCreatedAt(now);
        patient.setUpdatedAt(now);
        return patientRepository.save(patient);
    }

    @Override
    public Patient update(Long id, Patient patient) {
        Patient existing = getById(id);
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        if (patient.getFullName() != null) existing.setFullName(patient.getFullName());
        if (patient.getPhone() != null) existing.setPhone(patient.getPhone());
        if (patient.getAddress() != null) existing.setAddress(patient.getAddress());
        if (patient.getGender() != null) existing.setGender(patient.getGender());
        if (patient.getDateOfBirth() != null) existing.setDateOfBirth(patient.getDateOfBirth());
        if (patient.getIdentityNumber() != null) existing.setIdentityNumber(patient.getIdentityNumber());
        if (patient.getHealthInsuranceNumber() != null) existing.setHealthInsuranceNumber(patient.getHealthInsuranceNumber());
        if (patient.getOccupation() != null) existing.setOccupation(patient.getOccupation());
        if (patient.getEmergencyContactName() != null) existing.setEmergencyContactName(patient.getEmergencyContactName());
        if (patient.getEmergencyContactPhone() != null) existing.setEmergencyContactPhone(patient.getEmergencyContactPhone());
        if (patient.getBloodType() != null) existing.setBloodType(patient.getBloodType());
        if (patient.getAllergyNote() != null) existing.setAllergyNote(patient.getAllergyNote());
        if (patient.getMedicalHistory() != null) existing.setMedicalHistory(patient.getMedicalHistory());
        if (patient.getStatus() != null) existing.setStatus(patient.getStatus());
        if (patient.getPatientCode() != null && !patient.getPatientCode().isBlank()) {
            existing.setPatientCode(patient.getPatientCode());
        }
        existing.setUpdatedAt(now);
        return patientRepository.save(existing);
    }

    @Override
    public void delete(Long id) {
        Patient existing = getById(id);
        // Medical records are retained; deletion is represented as inactive.
        existing.setStatus(Patient.Status.INACTIVE);
        existing.setUpdatedAt(LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")));
        patientRepository.save(existing);
    }

    private String generateNextPatientCode() {
        long nextNumber = patientRepository.findAll().stream()
                .map(Patient::getPatientCode)
                .filter(code -> code != null && code.matches("(?i)^BN\\d+$"))
                .mapToLong(code -> {
                    try {
                        return Long.parseLong(code.substring(2));
                    } catch (NumberFormatException e) {
                        return 0L;
                    }
                })
                .max()
                .orElse(20000L);

        String candidate;
        long attempt = nextNumber + 1;
        do {
            candidate = "BN" + String.format("%05d", attempt++);
        } while (patientRepository.findByPatientCode(candidate).isPresent());
        return candidate;
    }
}
