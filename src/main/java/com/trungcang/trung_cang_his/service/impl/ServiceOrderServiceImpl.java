package com.trungcang.trung_cang_his.service.impl;

import com.trungcang.trung_cang_his.domain.ServiceOrder;
import com.trungcang.trung_cang_his.repository.ServiceOrderRepository;
import com.trungcang.trung_cang_his.service.ServiceOrderService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ServiceOrderServiceImpl implements ServiceOrderService {

    private final ServiceOrderRepository serviceOrderRepository;

    public ServiceOrderServiceImpl(ServiceOrderRepository serviceOrderRepository) {
        this.serviceOrderRepository = serviceOrderRepository;
    }

    @Override
    public List<ServiceOrder> getAll() {
        return serviceOrderRepository.findAll();
    }

    @Override
    public ServiceOrder getById(Long id) {
        return serviceOrderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Service order not found with id: " + id));
    }

    @Override
    public ServiceOrder create(ServiceOrder serviceOrder) {
        return serviceOrderRepository.save(serviceOrder);
    }

    @Override
    public ServiceOrder update(Long id, ServiceOrder serviceOrder) {
        ServiceOrder existing = getById(id);
        existing.setOrderCode(serviceOrder.getOrderCode());
        existing.setQuantity(serviceOrder.getQuantity());
        existing.setStatus(serviceOrder.getStatus());
        return serviceOrderRepository.save(existing);
    }

    @Override
    public void delete(Long id) {
        serviceOrderRepository.deleteById(id);
    }
}
