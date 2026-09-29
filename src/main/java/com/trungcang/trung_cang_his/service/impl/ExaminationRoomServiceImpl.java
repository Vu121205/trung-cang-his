package com.trungcang.trung_cang_his.service.impl;

import com.trungcang.trung_cang_his.domain.ExaminationRoom;
import com.trungcang.trung_cang_his.repository.ExaminationRoomRepository;
import com.trungcang.trung_cang_his.service.ExaminationRoomService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ExaminationRoomServiceImpl implements ExaminationRoomService {

    private final ExaminationRoomRepository examinationRoomRepository;

    public ExaminationRoomServiceImpl(ExaminationRoomRepository examinationRoomRepository) {
        this.examinationRoomRepository = examinationRoomRepository;
    }

    @Override
    public List<ExaminationRoom> getAll() {
        return examinationRoomRepository.findAll();
    }

    @Override
    public ExaminationRoom getById(Long id) {
        return examinationRoomRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Examination room not found with id: " + id));
    }

    @Override
    public ExaminationRoom create(ExaminationRoom examinationRoom) {
        return examinationRoomRepository.save(examinationRoom);
    }

    @Override
    public ExaminationRoom update(Long id, ExaminationRoom examinationRoom) {
        ExaminationRoom existing = getById(id);
        existing.setCode(examinationRoom.getCode());
        existing.setName(examinationRoom.getName());
        existing.setFloor(examinationRoom.getFloor());
        existing.setDepartment(examinationRoom.getDepartment());
        existing.setStatus(examinationRoom.getStatus());
        return examinationRoomRepository.save(existing);
    }

    @Override
    public void delete(Long id) {
        examinationRoomRepository.deleteById(id);
    }
}
