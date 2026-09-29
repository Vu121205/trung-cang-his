package com.trungcang.trung_cang_his.service.impl;

import com.trungcang.trung_cang_his.domain.Examination;
import com.trungcang.trung_cang_his.repository.ExaminationRepository;
import com.trungcang.trung_cang_his.service.ExaminationService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ExaminationServiceImpl implements ExaminationService {

    private final ExaminationRepository examinationRepository;

    public ExaminationServiceImpl(ExaminationRepository examinationRepository) {
        this.examinationRepository = examinationRepository;
    }

    @Override
    public List<Examination> getAll() {
        return examinationRepository.findAll();
    }

    @Override
    public Examination getById(Long id) {
        return examinationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Examination not found with id: " + id));
    }

    @Override
    public Examination create(Examination examination) {
        return examinationRepository.save(examination);
    }

    @Override
    public Examination update(Long id, Examination examination) {
        Examination existing = getById(id);
        existing.setChiefComplaint(examination.getChiefComplaint());
        existing.setSymptoms(examination.getSymptoms());
        existing.setMedicalHistory(examination.getMedicalHistory());
        existing.setPhysicalExamination(examination.getPhysicalExamination());
        existing.setClinicalNote(examination.getClinicalNote());
        existing.setAdvice(examination.getAdvice());
        existing.setExaminationResult(examination.getExaminationResult());
        existing.setStatus(examination.getStatus());
        return examinationRepository.save(existing);
    }

    @Override
    public void delete(Long id) {
        examinationRepository.deleteById(id);
    }
}
