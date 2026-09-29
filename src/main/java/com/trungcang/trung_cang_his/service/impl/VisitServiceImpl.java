package com.trungcang.trung_cang_his.service.impl;

import com.trungcang.trung_cang_his.domain.Visit;
import com.trungcang.trung_cang_his.repository.VisitRepository;
import com.trungcang.trung_cang_his.service.VisitService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class VisitServiceImpl implements VisitService {

    private final VisitRepository visitRepository;

    public VisitServiceImpl(VisitRepository visitRepository) {
        this.visitRepository = visitRepository;
    }

    @Override
    public List<Visit> getAll() {
        return visitRepository.findAll();
    }

    @Override
    public Visit getById(Long id) {
        return visitRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Visit not found with id: " + id));
    }

    @Override
    public Visit create(Visit visit) {
        return visitRepository.save(visit);
    }

    @Override
    public Visit update(Long id, Visit visit) {
        Visit existing = getById(id);
        existing.setVisitCode(visit.getVisitCode());
        existing.setReason(visit.getReason());
        existing.setStatus(visit.getStatus());
        return visitRepository.save(existing);
    }

    @Override
    public void delete(Long id) {
        visitRepository.deleteById(id);
    }
}
