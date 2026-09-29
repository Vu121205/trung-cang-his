package com.trungcang.trung_cang_his.service;

import com.trungcang.trung_cang_his.domain.ServiceResult;

import java.util.List;

public interface ServiceResultService {
    List<ServiceResult> getAll();
    ServiceResult getById(Long id);
    ServiceResult create(ServiceResult serviceResult);
    ServiceResult update(Long id, ServiceResult serviceResult);
    void delete(Long id);
}
