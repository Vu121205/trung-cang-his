package com.trungcang.trung_cang_his.controller;

import com.trungcang.trung_cang_his.domain.ExaminationRoom;
import com.trungcang.trung_cang_his.service.ExaminationRoomService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/examination-rooms")
public class ExaminationRoomController {

    private final ExaminationRoomService examinationRoomService;

    public ExaminationRoomController(ExaminationRoomService examinationRoomService) {
        this.examinationRoomService = examinationRoomService;
    }

    @GetMapping
    public List<ExaminationRoom> getAllExaminationRooms() {
        return examinationRoomService.getAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ExaminationRoom> getExaminationRoomById(@PathVariable Long id) {
        return ResponseEntity.ok(examinationRoomService.getById(id));
    }

    @PostMapping
    public ResponseEntity<ExaminationRoom> createExaminationRoom(@RequestBody ExaminationRoom examinationRoom) {
        return ResponseEntity.ok(examinationRoomService.create(examinationRoom));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ExaminationRoom> updateExaminationRoom(@PathVariable Long id, @RequestBody ExaminationRoom examinationRoom) {
        return ResponseEntity.ok(examinationRoomService.update(id, examinationRoom));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteExaminationRoom(@PathVariable Long id) {
        examinationRoomService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
