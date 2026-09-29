package com.trungcang.trung_cang_his.service;

import com.trungcang.trung_cang_his.domain.ExaminationRoom;

import java.util.List;

public interface ExaminationRoomService {
    List<ExaminationRoom> getAll();
    ExaminationRoom getById(Long id);
    ExaminationRoom create(ExaminationRoom examinationRoom);
    ExaminationRoom update(Long id, ExaminationRoom examinationRoom);
    void delete(Long id);
}
