package com.trungcang.trung_cang_his.controller;

import com.trungcang.trung_cang_his.domain.Examination;
import com.trungcang.trung_cang_his.service.ExaminationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/examinations")
public class ExaminationController {

    private final ExaminationService examinationService;

    public ExaminationController(ExaminationService examinationService) {
        this.examinationService = examinationService;
    }

    @GetMapping
    public List<Examination> getAllExaminations() {
        return examinationService.getAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Examination> getExaminationById(@PathVariable Long id) {
        return ResponseEntity.ok(examinationService.getById(id));
    }

    @PostMapping
    public ResponseEntity<Examination> createExamination(@RequestBody Examination examination) {
        return ResponseEntity.ok(examinationService.create(examination));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Examination> updateExamination(@PathVariable Long id, @RequestBody Examination examination) {
        return ResponseEntity.ok(examinationService.update(id, examination));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteExamination(@PathVariable Long id) {
        examinationService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
