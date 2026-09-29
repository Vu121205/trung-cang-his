package com.trungcang.trung_cang_his.service;

import com.trungcang.trung_cang_his.domain.ServiceOrder;

import java.util.List;

public interface ServiceOrderService {
    List<ServiceOrder> getAll();
    ServiceOrder getById(Long id);
    ServiceOrder create(ServiceOrder serviceOrder);
    ServiceOrder update(Long id, ServiceOrder serviceOrder);
    void delete(Long id);
}
