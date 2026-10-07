package com.trungcang.trung_cang_his.controller;

import com.trungcang.trung_cang_his.service.ExaminationHistoryService;
import com.trungcang.trung_cang_his.domain.Examination;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
public class ExaminationHistoryController {
    private final ExaminationHistoryService history;

    @GetMapping("/api/examination-history")
    public ExaminationHistoryService.HistoryPage list(@RequestParam(defaultValue = "") String q,
            @RequestParam(required = false) LocalDate from, @RequestParam(required = false) LocalDate to,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return history.search(q, from, to, page, size);
    }

    @GetMapping("/api/examination-history/{id}")
    public ExaminationHistoryService.HistoryDetail detail(@PathVariable Long id) { return history.detail(id); }

    @PostMapping("/api/examination-history/{id}/paper-status")
    public ExaminationHistoryService.HistoryDetail paperStatus(@PathVariable Long id,
            @RequestParam Examination.PaperRecordStatus status, Authentication authentication) {
        return history.updatePaperRecordStatus(id, status, authentication.getName());
    }

    @PostMapping("/api/examinations/complete")
    public ExaminationHistoryService.HistoryDetail complete(@Valid @RequestBody ExaminationHistoryService.CompleteRequest request,
                                                             Authentication authentication) {
        return history.complete(request, authentication.getName());
    }

    @PostMapping("/api/examination-history/{id}/reopen")
    public ExaminationHistoryService.HistoryDetail reopen(@PathVariable Long id, Authentication authentication) {
        return history.reopen(id, authentication.getName());
    }

    @GetMapping("/api/examination-history/{id}/reopen-eligibility")
    public boolean canReopen(@PathVariable Long id) { return history.canReopen(id); }
}
