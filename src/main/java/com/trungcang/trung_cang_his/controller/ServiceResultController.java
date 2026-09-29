package com.trungcang.trung_cang_his.controller;

import com.trungcang.trung_cang_his.domain.ServiceResult;
import com.trungcang.trung_cang_his.service.ServiceResultService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/service-results")
public class ServiceResultController {

    private final ServiceResultService serviceResultService;

    public ServiceResultController(ServiceResultService serviceResultService) {
        this.serviceResultService = serviceResultService;
    }

    @GetMapping
    public List<ServiceResult> getAllServiceResults() {
        return serviceResultService.getAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ServiceResult> getServiceResultById(@PathVariable Long id) {
        return ResponseEntity.ok(serviceResultService.getById(id));
    }

    @PostMapping
    public ResponseEntity<ServiceResult> createServiceResult(@RequestBody ServiceResult serviceResult) {
        return ResponseEntity.ok(serviceResultService.create(serviceResult));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ServiceResult> updateServiceResult(@PathVariable Long id, @RequestBody ServiceResult serviceResult) {
        return ResponseEntity.ok(serviceResultService.update(id, serviceResult));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteServiceResult(@PathVariable Long id) {
        serviceResultService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
