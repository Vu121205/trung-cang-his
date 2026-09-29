package com.trungcang.trung_cang_his.service;

import com.trungcang.trung_cang_his.domain.Visit;

import java.util.List;

public interface VisitService {
    List<Visit> getAll();
    Visit getById(Long id);
    Visit create(Visit visit);
    Visit update(Long id, Visit visit);
    void delete(Long id);
}
