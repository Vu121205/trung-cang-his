package com.trungcang.trung_cang_his.service.impl;

import com.trungcang.trung_cang_his.domain.Diagnosis;
import com.trungcang.trung_cang_his.repository.DiagnosisRepository;
import com.trungcang.trung_cang_his.service.DiagnosisService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DiagnosisServiceImpl implements DiagnosisService {

    private final DiagnosisRepository diagnosisRepository;

    public DiagnosisServiceImpl(DiagnosisRepository diagnosisRepository) {
        this.diagnosisRepository = diagnosisRepository;
    }

    @Override
    public List<Diagnosis> getAll() {
        return diagnosisRepository.findAll();
    }

    @Override
    public Diagnosis getById(Long id) {
        return diagnosisRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Diagnosis not found with id: " + id));
    }

    @Override
    public Diagnosis create(Diagnosis diagnosis) {
        return diagnosisRepository.save(diagnosis);
    }

    @Override
    public Diagnosis update(Long id, Diagnosis diagnosis) {
        Diagnosis existing = getById(id);
        existing.setName(diagnosis.getName());
        existing.setDescription(diagnosis.getDescription());
        existing.setStatus(diagnosis.getStatus());
        return diagnosisRepository.save(existing);
    }

    @Override
    public void delete(Long id) {
        diagnosisRepository.deleteById(id);
    }
}
