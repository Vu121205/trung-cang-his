package com.trungcang.trung_cang_his.service.impl;

import com.trungcang.trung_cang_his.domain.MedicalService;
import com.trungcang.trung_cang_his.repository.MedicalServiceRepository;
import com.trungcang.trung_cang_his.service.MedicalServiceService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MedicalServiceServiceImpl implements MedicalServiceService {

    private final MedicalServiceRepository medicalServiceRepository;

    public MedicalServiceServiceImpl(MedicalServiceRepository medicalServiceRepository) {
        this.medicalServiceRepository = medicalServiceRepository;
    }

    @Override
    public List<MedicalService> getAll() {
        return medicalServiceRepository.findAll();
    }

    @Override
    public MedicalService getById(Long id) {
        return medicalServiceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Medical service not found with id: " + id));
    }

    @Override
    public MedicalService create(MedicalService medicalService) {
        return medicalServiceRepository.save(medicalService);
    }

    @Override
    public MedicalService update(Long id, MedicalService medicalService) {
        MedicalService existing = getById(id);
        existing.setName(medicalService.getName());
        existing.setPrice(medicalService.getPrice());
        existing.setStatus(medicalService.getStatus());
        return medicalServiceRepository.save(existing);
    }

    @Override
    public void delete(Long id) {
        medicalServiceRepository.deleteById(id);
    }
}
