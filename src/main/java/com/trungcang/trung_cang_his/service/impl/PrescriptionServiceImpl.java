package com.trungcang.trung_cang_his.service.impl;

import com.trungcang.trung_cang_his.domain.Prescription;
import com.trungcang.trung_cang_his.repository.PrescriptionRepository;
import com.trungcang.trung_cang_his.service.PrescriptionService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PrescriptionServiceImpl implements PrescriptionService {

    private final PrescriptionRepository prescriptionRepository;

    public PrescriptionServiceImpl(PrescriptionRepository prescriptionRepository) {
        this.prescriptionRepository = prescriptionRepository;
    }

    @Override
    public List<Prescription> getAll() {
        return prescriptionRepository.findAll();
    }

    @Override
    public Prescription getById(Long id) {
        return prescriptionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Prescription not found with id: " + id));
    }

    @Override
    public Prescription create(Prescription prescription) {
        return prescriptionRepository.save(prescription);
    }

    @Override
    public Prescription update(Long id, Prescription prescription) {
        Prescription existing = getById(id);
        existing.setPrescriptionCode(prescription.getPrescriptionCode());
        existing.setStatus(prescription.getStatus());
        existing.setNote(prescription.getNote());
        return prescriptionRepository.save(existing);
    }

    @Override
    public void delete(Long id) {
        prescriptionRepository.deleteById(id);
    }
}
