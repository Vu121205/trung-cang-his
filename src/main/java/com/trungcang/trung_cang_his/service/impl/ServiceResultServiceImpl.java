package com.trungcang.trung_cang_his.service.impl;

import com.trungcang.trung_cang_his.domain.ServiceResult;
import com.trungcang.trung_cang_his.repository.ServiceResultRepository;
import com.trungcang.trung_cang_his.service.ServiceResultService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ServiceResultServiceImpl implements ServiceResultService {

    private final ServiceResultRepository serviceResultRepository;

    public ServiceResultServiceImpl(ServiceResultRepository serviceResultRepository) {
        this.serviceResultRepository = serviceResultRepository;
    }

    @Override
    public List<ServiceResult> getAll() {
        return serviceResultRepository.findAll();
    }

    @Override
    public ServiceResult getById(Long id) {
        return serviceResultRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Service result not found with id: " + id));
    }

    @Override
    public ServiceResult create(ServiceResult serviceResult) {
        return serviceResultRepository.save(serviceResult);
    }

    @Override
    public ServiceResult update(Long id, ServiceResult serviceResult) {
        ServiceResult existing = getById(id);
        existing.setResultDescription(serviceResult.getResultDescription());
        existing.setConclusion(serviceResult.getConclusion());
        existing.setStatus(serviceResult.getStatus());
        return serviceResultRepository.save(existing);
    }

    @Override
    public void delete(Long id) {
        serviceResultRepository.deleteById(id);
    }
}
