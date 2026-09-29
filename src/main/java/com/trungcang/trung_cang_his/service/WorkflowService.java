package com.trungcang.trung_cang_his.service;

import com.trungcang.trung_cang_his.domain.*;
import com.trungcang.trung_cang_his.repository.*;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WorkflowService {
    private final VisitRepository visits;
    private final PrescriptionRepository prescriptions;
    private final PrescriptionDetailRepository prescriptionDetails;
    private final InvoiceRepository invoices;
    private final InvoiceDetailRepository invoiceDetails;
        private final MedicalServiceRepository medicalServices;
        private final MedicineRepository medicines;
    private final MedicineBatchRepository batches;
    private final UserRepository users;

    @Transactional(readOnly = true)
    public List<BillingItem> pendingPayments() {
        return visits.findAllByStatusOrderByVisitDateAscVisitTimeAsc(Visit.Status.WAITING_PAYMENT).stream()
                .filter(visit -> invoices.findFirstByVisit_IdOrderByIdDesc(visit.getId())
                        .map(invoice -> invoice.getStatus() != Invoice.Status.PAID).orElse(true))
                .map(this::billingItem).toList();
    }

    @Transactional
        public PaymentReceipt pay(Long visitId, PaymentRequest request, String username) {
        Visit visit = visits.findById(visitId).orElseThrow(() -> missing("Không tìm thấy lượt khám."));
        if (visit.getStatus() != Visit.Status.WAITING_PAYMENT) throw conflict("Lượt khám không còn chờ thanh toán.");
        if (invoices.findFirstByVisit_IdOrderByIdDesc(visitId).filter(i -> i.getStatus() == Invoice.Status.PAID).isPresent()) {
            throw conflict("Lượt khám đã được thanh toán.");
        }
        List<BillingLine> lines = billingLines(visit);
        BigDecimal total = lines.stream().map(BillingLine::totalPrice).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (total.signum() <= 0) throw conflict("Chưa cấu hình khoản thu cho lượt khám này.");
        LocalDateTime now = LocalDateTime.now();
        Invoice invoice = new Invoice();
        invoice.setInvoiceCode("HD-" + visit.getVisitCode());
        invoice.setVisit(visit);
        invoice.setPatient(visit.getPatient());
        invoice.setCashier(users.findByUsername(username).orElseThrow(() -> missing("Không tìm thấy nhân viên thu ngân.")));
        invoice.setInvoiceDate(now);
        invoice.setSubtotal(total);
        invoice.setTotalAmount(total);
        invoice.setPaidAmount(total);
        invoice.setDebtAmount(BigDecimal.ZERO);
        invoice.setPaymentMethod(request.method());
        invoice.setStatus(Invoice.Status.PAID);
        invoices.save(invoice);
                for (BillingLine line : lines) {
            InvoiceDetail detail = new InvoiceDetail();
            detail.setInvoice(invoice);
                        detail.setItemType(InvoiceDetail.ItemType.valueOf(line.itemType()));
                            if (line.medicineId() != null) detail.setMedicine(medicines.getReferenceById(line.medicineId()));
                            if (line.serviceId() != null) detail.setService(medicalServices.getReferenceById(line.serviceId()));
                        detail.setDescription(line.description());
                        detail.setQuantity(line.quantity());
                        detail.setUnit(line.unit());
                        detail.setUnitPrice(line.unitPrice());
                        detail.setTotalPrice(line.totalPrice());
            invoiceDetails.save(detail);
        }
        return new PaymentReceipt(invoice.getId(), invoice.getInvoiceCode(), total, invoice.getPaymentMethod(), invoice.getStatus());
    }

    @Transactional(readOnly = true)
    public List<DispenseItem> pendingDispensing() {
        return prescriptions.findAllByStatusOrderByPrescriptionDateAsc(Prescription.Status.PRESCRIBED).stream()
                .filter(prescription -> invoices.findFirstByVisit_IdOrderByIdDesc(prescription.getVisit().getId())
                        .map(invoice -> invoice.getStatus() == Invoice.Status.PAID).orElse(false))
                .map(this::dispenseItem).toList();
    }

    @Transactional
        public DispenseResult dispense(Long prescriptionId) {
        Prescription prescription = prescriptions.findById(prescriptionId)
                .orElseThrow(() -> missing("Không tìm thấy đơn thuốc."));
        if (prescription.getStatus() != Prescription.Status.PRESCRIBED) throw conflict("Đơn thuốc không còn chờ cấp phát.");
        Invoice invoice = invoices.findFirstByVisit_IdOrderByIdDesc(prescription.getVisit().getId())
                .filter(value -> value.getStatus() == Invoice.Status.PAID)
                .orElseThrow(() -> conflict("Chỉ được cấp thuốc sau khi hóa đơn đã thanh toán."));
        List<PrescriptionDetail> lines = prescriptionDetails.findAllByPrescription_Visit_Id(prescription.getVisit().getId()).stream()
                .filter(line -> line.getPrescription().getId().equals(prescriptionId)).toList();
        if (lines.isEmpty()) throw conflict("Đơn thuốc không có thuốc để cấp phát.");
        LocalDate today = LocalDate.now();
        for (PrescriptionDetail line : lines) {
            int remaining = line.getQuantity();
            List<MedicineBatch> available = batches.findAvailableForDispensing(line.getMedicine().getId(), today);
            int total = available.stream().mapToInt(MedicineBatch::getQuantity).sum();
            if (total < remaining) throw conflict("Tồn kho không đủ thuốc " + line.getMedicine().getName() + ".");
            for (MedicineBatch batch : available) {
                int used = Math.min(remaining, batch.getQuantity());
                batch.setQuantity(batch.getQuantity() - used);
                remaining -= used;
                if (remaining == 0) break;
            }
        }
        prescription.setStatus(Prescription.Status.DISPENSED);
        prescription.getVisit().setStatus(Visit.Status.COMPLETED);
        prescription.getVisit().setUpdatedAt(LocalDateTime.now());
        return new DispenseResult(prescription.getId(), prescription.getPrescriptionCode(), prescription.getStatus(),
                prescription.getVisit().getStatus());
    }

    private BillingItem billingItem(Visit visit) {
        List<BillingLine> items = billingLines(visit);
        BigDecimal total = items.stream().map(BillingLine::totalPrice).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new BillingItem(visit.getId(), visit.getVisitCode(), visit.getPatient().getPatientCode(),
                visit.getPatient().getFullName(), visit.getVisitDate(), items, total);
    }

        private List<BillingLine> billingLines(Visit visit) {
                List<BillingLine> items = new java.util.ArrayList<>();
                String roomCode = visit.getRoom().getCode();
                if (roomCode != null && roomCode.startsWith("PK-")) {
                        String serviceCode = roomCode.replaceFirst("^PK-", "KB-").replaceFirst("-\\d+$", "");
                        medicalServices.findByCodeAndStatus(serviceCode, MedicalService.Status.ACTIVE).ifPresent(service ->
                                        items.add(new BillingLine("EXAMINATION", service.getId(), null, service.getName(), 1, service.getUnit(),
                                                        service.getPrice(), service.getPrice())));
                }
                for (PrescriptionDetail detail : prescriptionDetails.findAllByPrescription_Visit_Id(visit.getId())) {
                        items.add(new BillingLine("MEDICINE", null, detail.getMedicine().getId(), detail.getMedicine().getName(),
                                        detail.getQuantity(), detail.getUnit(), detail.getUnitPrice(), detail.getTotalPrice()));
                }
                return items;
        }

    private DispenseItem dispenseItem(Prescription prescription) {
        List<DispenseLine> lines = prescriptionDetails.findAllByPrescription_Visit_Id(prescription.getVisit().getId()).stream()
                .filter(line -> line.getPrescription().getId().equals(prescription.getId()))
                .map(line -> new DispenseLine(line.getMedicine().getName(), line.getMedicine().getStrength(), line.getQuantity(),
                        line.getUnit(), line.getInstruction(), batches.countAvailableForDispensing(line.getMedicine().getId(), LocalDate.now())))
                .toList();
        return new DispenseItem(prescription.getId(), prescription.getPrescriptionCode(), prescription.getVisit().getPatient().getPatientCode(),
            prescription.getVisit().getPatient().getFullName(), prescription.getDoctor().getFullName(),
            prescription.getExamination() == null ? "—" : prescription.getExamination().getExaminationResult(), lines);
    }

    private ResponseStatusException missing(String message) { return new ResponseStatusException(HttpStatus.NOT_FOUND, message); }
    private ResponseStatusException conflict(String message) { return new ResponseStatusException(HttpStatus.CONFLICT, message); }

        public record BillingLine(String itemType, Long serviceId, Long medicineId, String description, Integer quantity,
                                                          String unit, BigDecimal unitPrice, BigDecimal totalPrice) {}
    public record BillingItem(Long visitId, String visitCode, String patientCode, String patientName, LocalDate visitDate,
                              List<BillingLine> lines, BigDecimal totalAmount) {}
        public record PaymentRequest(@NotNull Invoice.PaymentMethod method) {}
        public record PaymentReceipt(Long invoiceId, String invoiceCode, BigDecimal totalAmount,
                                                                 Invoice.PaymentMethod method, Invoice.Status status) {}
        public record DispenseLine(String medicineName, String strength, Integer quantity, String unit, String instruction, Long stock) {}
    public record DispenseItem(Long prescriptionId, String prescriptionCode, String patientCode, String patientName,
                                   String doctorName, String diagnosis, List<DispenseLine> lines) {}
        public record DispenseResult(Long prescriptionId, String prescriptionCode, Prescription.Status prescriptionStatus,
                                                                 Visit.Status visitStatus) {}
}