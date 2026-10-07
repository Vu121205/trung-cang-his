package com.trungcang.trung_cang_his.controller;

import com.trungcang.trung_cang_his.domain.Prescription;
import com.trungcang.trung_cang_his.service.PrescriptionService;
import com.trungcang.trung_cang_his.service.AuditService;
import org.springframework.security.core.Authentication;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/prescriptions")
public class PrescriptionController {

    private final PrescriptionService prescriptionService;
    private final AuditService auditService;

    public PrescriptionController(PrescriptionService prescriptionService, AuditService auditService) {
        this.prescriptionService = prescriptionService;
        this.auditService = auditService;
    }

    @GetMapping
    public List<Prescription> getAllPrescriptions() {
        return prescriptionService.getAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Prescription> getPrescriptionById(@PathVariable Long id) {
        return ResponseEntity.ok(prescriptionService.getById(id));
    }

    @GetMapping("/{id}/print")
    public ResponseEntity<PrescriptionService.PrescriptionPrintData> printData(@PathVariable Long id) {
        return ResponseEntity.ok(prescriptionService.getPrintData(id));
    }

    @PostMapping
    public ResponseEntity<Prescription> createPrescription(@RequestBody Prescription prescription,
                                                            Authentication authentication) {
        Prescription saved = prescriptionService.create(prescription);
        auditService.record("PRESCRIPTION_CREATED", "Prescription", saved.getId(), authentication.getName(),
                "Tạo đơn thuốc.");
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Prescription> updatePrescription(@PathVariable Long id, @RequestBody Prescription prescription,
                                                            Authentication authentication) {
        Prescription saved = prescriptionService.update(id, prescription);
        auditService.record("PRESCRIPTION_UPDATED", "Prescription", id, authentication.getName(),
                "Cập nhật đơn thuốc.");
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePrescription(@PathVariable Long id, Authentication authentication) {
        prescriptionService.delete(id);
        auditService.record("PRESCRIPTION_CANCELLED", "Prescription", id, authentication.getName(),
                "Đơn thuốc được hủy mềm; không xóa lịch sử kê đơn.");
        return ResponseEntity.noContent().build();
    }
}
