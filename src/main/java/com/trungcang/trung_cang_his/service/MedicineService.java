package com.trungcang.trung_cang_his.service;

import com.trungcang.trung_cang_his.domain.Medicine;

import java.util.List;

public interface MedicineService {
    List<Medicine> getAll();
    Medicine getById(Long id);
    Medicine create(Medicine medicine);
    Medicine update(Long id, Medicine medicine);
    void delete(Long id);
}
