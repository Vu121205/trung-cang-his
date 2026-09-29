package com.trungcang.trung_cang_his.service;

import com.trungcang.trung_cang_his.domain.Examination;

import java.util.List;

public interface ExaminationService {
    List<Examination> getAll();
    Examination getById(Long id);
    Examination create(Examination examination);
    Examination update(Long id, Examination examination);
    void delete(Long id);
}
