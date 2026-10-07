package com.trungcang.trung_cang_his.controller;

import com.trungcang.trung_cang_his.domain.Patient;
import com.trungcang.trung_cang_his.service.PatientService;
import com.trungcang.trung_cang_his.service.AuditService;
import org.springframework.security.core.Authentication;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/patients")
public class PatientController {

    private final PatientService patientService;
    private final AuditService auditService;

    public PatientController(PatientService patientService, AuditService auditService) {
        this.patientService = patientService;
        this.auditService = auditService;
    }

    @GetMapping
    public List<Patient> getAllPatients() {
        return patientService.getAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Patient> getPatientById(@PathVariable Long id) {
        return ResponseEntity.ok(patientService.getById(id));
    }

    @PostMapping
    public ResponseEntity<Patient> createPatient(@RequestBody Patient patient, Authentication authentication) {
        Patient saved = patientService.create(patient);
        auditService.record("PATIENT_CREATED", "Patient", saved.getId(), authentication.getName(),
                "Tạo hồ sơ bệnh nhân.");
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Patient> updatePatient(@PathVariable Long id, @RequestBody Patient patient,
                                                 Authentication authentication) {
        Patient saved = patientService.update(id, patient);
        auditService.record("PATIENT_UPDATED", "Patient", id, authentication.getName(),
                "Cập nhật hồ sơ bệnh nhân.");
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePatient(@PathVariable Long id, Authentication authentication) {
        patientService.delete(id);
        auditService.record("PATIENT_DEACTIVATED", "Patient", id, authentication.getName(),
                "Hồ sơ được ngừng hoạt động; dữ liệu lịch sử không bị xóa vật lý.");
        return ResponseEntity.noContent().build();
    }
}
