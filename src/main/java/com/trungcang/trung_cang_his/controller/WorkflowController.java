package com.trungcang.trung_cang_his.controller;

import com.trungcang.trung_cang_his.service.WorkflowService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/workflow")
@RequiredArgsConstructor
public class WorkflowController {
    private final WorkflowService workflow;

    @GetMapping("/billing")
    public List<WorkflowService.BillingItem> pendingPayments() { return workflow.pendingPayments(); }

    @PostMapping("/billing/{visitId}/pay")
    public WorkflowService.PaymentReceipt pay(@PathVariable Long visitId,
                       @Valid @RequestBody WorkflowService.PaymentRequest request, Authentication authentication) {
        return workflow.pay(visitId, request, authentication.getName());
    }

    @GetMapping("/dispensing")
    public List<WorkflowService.DispenseItem> pendingDispensing() { return workflow.pendingDispensing(); }

    @PostMapping("/dispensing/{prescriptionId}/confirm")
    public WorkflowService.DispenseResult dispense(@PathVariable Long prescriptionId, Authentication authentication) {
        return workflow.dispense(prescriptionId, authentication.getName());
    }
}
