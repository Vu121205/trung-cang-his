package com.trungcang.trung_cang_his.controller;

import com.trungcang.trung_cang_his.domain.ServiceOrder;
import com.trungcang.trung_cang_his.service.ServiceOrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/service-orders")
public class ServiceOrderController {

    private final ServiceOrderService serviceOrderService;

    public ServiceOrderController(ServiceOrderService serviceOrderService) {
        this.serviceOrderService = serviceOrderService;
    }

    @GetMapping
    public List<ServiceOrder> getAllServiceOrders() {
        return serviceOrderService.getAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ServiceOrder> getServiceOrderById(@PathVariable Long id) {
        return ResponseEntity.ok(serviceOrderService.getById(id));
    }

    @PostMapping
    public ResponseEntity<ServiceOrder> createServiceOrder(@RequestBody ServiceOrder serviceOrder) {
        return ResponseEntity.ok(serviceOrderService.create(serviceOrder));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ServiceOrder> updateServiceOrder(@PathVariable Long id, @RequestBody ServiceOrder serviceOrder) {
        return ResponseEntity.ok(serviceOrderService.update(id, serviceOrder));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteServiceOrder(@PathVariable Long id) {
        serviceOrderService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
