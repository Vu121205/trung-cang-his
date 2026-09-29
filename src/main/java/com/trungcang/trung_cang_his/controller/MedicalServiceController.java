package com.trungcang.trung_cang_his.controller;

import com.trungcang.trung_cang_his.domain.MedicalService;
import com.trungcang.trung_cang_his.service.MedicalServiceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/medical-services")
public class MedicalServiceController {

    private final MedicalServiceService medicalServiceService;

    public MedicalServiceController(MedicalServiceService medicalServiceService) {
        this.medicalServiceService = medicalServiceService;
    }

    @GetMapping
    public List<MedicalService> getAllMedicalServices() {
        return medicalServiceService.getAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<MedicalService> getMedicalServiceById(@PathVariable Long id) {
        return ResponseEntity.ok(medicalServiceService.getById(id));
    }

    @PostMapping
    public ResponseEntity<MedicalService> createMedicalService(@RequestBody MedicalService medicalService) {
        return ResponseEntity.ok(medicalServiceService.create(medicalService));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MedicalService> updateMedicalService(@PathVariable Long id, @RequestBody MedicalService medicalService) {
        return ResponseEntity.ok(medicalServiceService.update(id, medicalService));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMedicalService(@PathVariable Long id) {
        medicalServiceService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
