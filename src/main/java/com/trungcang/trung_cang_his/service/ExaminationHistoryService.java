package com.trungcang.trung_cang_his.service;

import com.trungcang.trung_cang_his.domain.*;
import com.trungcang.trung_cang_his.repository.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExaminationHistoryService {
    private final ExaminationRepository examinations;
    private final VisitRepository visits;
    private final PatientRepository patients;
    private final UserRepository users;
    private final ExaminationRoomRepository rooms;
    private final DiagnosisRepository diagnoses;
    private final ExaminationDiagnosisRepository examinationDiagnoses;
    private final MedicineRepository medicines;
    private final PrescriptionRepository prescriptions;
    private final PrescriptionDetailRepository prescriptionDetails;
    private final VitalSignRepository vitalSigns;
    private final AuditService auditService;
    private final InvoiceRepository invoices;

    public HistoryPage search(String query, LocalDate from, LocalDate to, int page, int size) {
        if (from != null && to != null && from.isAfter(to)) throw badRequest("Từ ngày phải trước hoặc bằng đến ngày.");
        if (page < 0 || size < 1 || size > 100) throw badRequest("Phân trang không hợp lệ.");
        String keyword = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        Specification<Examination> spec = (root, criteria, cb) -> {
            var conditions = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            var visit = root.join("visit");
            var patient = visit.join("patient");
            if (!keyword.isEmpty()) {
                String pattern = "%" + keyword.replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
                conditions.add(cb.or(cb.like(cb.lower(patient.get("fullName")), pattern, '!'),
                        cb.like(cb.lower(patient.get("patientCode")), pattern, '!'),
                        cb.like(patient.get("phone"), pattern, '!')));
            }
            if (from != null) conditions.add(cb.greaterThanOrEqualTo(visit.get("visitDate"), from));
            if (to != null) conditions.add(cb.lessThanOrEqualTo(visit.get("visitDate"), to));
            return cb.and(conditions.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
        var result = examinations.findAll(spec, PageRequest.of(page, size, Sort.by("examinedAt", "id").descending()));
        return new HistoryPage(result.getContent().stream().map(this::summary).toList(), result.getTotalElements(), result.getTotalPages(), page);
    }

    public HistoryDetail detail(Long id) {
        Examination exam = examinations.findById(id).orElseThrow(() -> notFound("Không tìm thấy hồ sơ khám."));
        var diagnosisItems = examinationDiagnoses.findByExamination_Id(id).stream()
                .map(item -> new DiagnosisItem(item.getDiagnosis().getIcdCode(), item.getDiagnosis().getName())).toList();
        var medicineItems = prescriptionDetails.findByPrescription_Examination_Id(id).stream()
                .map(item -> new MedicineItem(item.getMedicine().getId(), item.getMedicine().getName(),
                        item.getMedicine().getInventoryType() == null ? Medicine.InventoryType.MEDICINE.name() : item.getMedicine().getInventoryType().name(),
                        item.getQuantity(), item.getUnit(), item.getDosage(), item.getFrequency(), item.getDuration(), item.getRoute(), item.getInstruction())).toList();
        VitalSummary vitalSummary = vitalSigns.findByVisit_Id(exam.getVisit().getId())
            .map(sign -> new VitalSummary(sign.getBloodPressureSystolic(), sign.getBloodPressureDiastolic(), sign.getPulse(),
                sign.getTemperature(), sign.getWeight())).orElse(null);
        Long prescriptionId = prescriptions.findByExamination_Id(id).map(Prescription::getId).orElse(null);
        return new HistoryDetail(summary(exam), exam.getSymptoms(), exam.getMedicalHistory(),
            exam.getClinicalNote(), exam.getExaminationResult(), exam.getAdvice(), diagnosisItems, medicineItems,
            vitalSummary, paperStatus(exam).name(), prescriptionId);
    }

    private HistoryRow summary(Examination exam) {
        Visit visit = exam.getVisit();
        Patient patient = visit.getPatient();
        return new HistoryRow(exam.getId(), visit.getVisitCode(), patient.getPatientCode(), patient.getFullName(),
                patient.getPhone(), visit.getVisitDate(), exam.getExaminedAt(), visit.getRoom().getName(),
                exam.getDoctor().getFullName(), exam.getStatus().name(), exam.getExaminationResult());
    }

    @Transactional
    public HistoryDetail complete(CompleteRequest request, String username) {
        Patient patient = patients.lockByCode(request.patientCode()).orElseThrow(() -> notFound("Không tìm thấy bệnh nhân."));
        User doctor = users.findByUsername(username).orElseThrow(() -> notFound("Không tìm thấy người khám."));
        String visitCode = "KB-" + request.requestId();
        var existing = examinations.findByVisit_VisitCode(visitCode);
        if (existing.isPresent()) {
            Examination exam = existing.get();
            if (!exam.getVisit().getPatient().getId().equals(patient.getId()) || !exam.getDoctor().getId().equals(doctor.getId())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Mã yêu cầu đã được sử dụng cho hồ sơ khác.");
            }
            if (exam.getVisit().getStatus() != Visit.Status.EXAMINING) return detail(exam.getId());
            ensureInvoiceNotPaid(exam.getVisit());
            updateExisting(exam, request, doctor, username);
            return detail(exam.getId());
        }
        ExaminationRoom room = rooms.findById(request.roomId()).filter(r -> r.getStatus() == ExaminationRoom.Status.ACTIVE)
                .orElseThrow(() -> badRequest("Vui lòng chọn phòng khám đang hoạt động."));
        Diagnosis diagnosis = diagnoses.findByIcdCode(request.icdCode().trim().toUpperCase(Locale.ROOT))
                .filter(d -> d.getStatus() == Diagnosis.Status.ACTIVE).orElseThrow(() -> badRequest("Mã ICD không có trong danh mục đang hoạt động."));
        LocalDateTime now = LocalDateTime.now();
        Visit visit = new Visit();
        visit.setVisitCode(visitCode);
        visit.setPatient(patient);
        visit.setRoom(room);
        visit.setDoctor(doctor);
        visit.setCreatedBy(doctor);
        visit.setVisitDate(now.toLocalDate());
        visit.setVisitTime(now.toLocalTime());
        visit.setReason(request.symptoms());
        visit.setStatus(Visit.Status.WAITING_PAYMENT);
        visit.setCreatedAt(now);
        visit.setUpdatedAt(now);
        visits.save(visit);
        Examination exam = new Examination();
        exam.setVisit(visit);
        exam.setDoctor(doctor);
        exam.setSymptoms(request.symptoms());
        exam.setMedicalHistory(request.medicalHistory());
        exam.setClinicalNote(request.clinicalNote());
        exam.setExaminationResult(diagnosis.getName());
        exam.setAdvice(request.advice());
        exam.setStatus(Examination.Status.COMPLETED);
        exam.setPaperRecordStatus(Examination.PaperRecordStatus.PENDING_PRINT);
        exam.setExaminedAt(now);
        exam.setCreatedAt(now);
        exam.setUpdatedAt(now);
        examinations.save(exam);
        if (request.vitalSigns() != null) {
            VitalSign vitalSign = new VitalSign();
            vitalSign.setVisit(visit);
            vitalSign.setBloodPressureSystolic(request.vitalSigns().systolic());
            vitalSign.setBloodPressureDiastolic(request.vitalSigns().diastolic());
            vitalSign.setPulse(request.vitalSigns().pulse());
            vitalSign.setTemperature(request.vitalSigns().temperature());
            vitalSign.setWeight(request.vitalSigns().weight());
            vitalSign.setMeasuredBy(doctor);
            vitalSign.setMeasuredAt(now);
            vitalSigns.save(vitalSign);
        }
        ExaminationDiagnosis examDiagnosis = new ExaminationDiagnosis();
        examDiagnosis.setExamination(exam);
        examDiagnosis.setDiagnosis(diagnosis);
        examinationDiagnoses.save(examDiagnosis);
        if (!request.medicines().isEmpty() || !request.supplies().isEmpty()) {
            Prescription prescription = new Prescription();
            prescription.setPrescriptionCode(prescriptionCode());
            prescription.setVisit(visit);
            prescription.setExamination(exam);
            prescription.setDoctor(doctor);
            prescription.setPrescriptionDate(now);
            prescription.setStatus(Prescription.Status.PRESCRIBED);
            prescription.setPaperRecordStatus(Prescription.PaperRecordStatus.PENDING_PRINT);
            prescriptions.save(prescription);
            BigDecimal total = BigDecimal.ZERO;
            for (MedicineRequest item : request.medicines()) total = total.add(addPrescriptionLine(prescription, item, Medicine.InventoryType.MEDICINE));
            for (SupplyRequest item : request.supplies()) total = total.add(addPrescriptionLine(prescription, item.supplyId(), item.quantity(), item.instruction(), Medicine.InventoryType.SUPPLY));
            prescription.setTotalAmount(total);
        }
        auditService.record("EXAMINATION_COMPLETED", "Examination", exam.getId(), username,
                "Hoàn tất hồ sơ khám; hồ sơ giấy cần được in và ký tay ngoài hệ thống.");
        return detail(exam.getId());
    }

    @Transactional
    public HistoryDetail reopen(Long examinationId, String username) {
        Examination exam = examinations.findById(examinationId).orElseThrow(() -> notFound("Không tìm thấy hồ sơ khám."));
        Visit visit = visits.findByIdForUpdate(exam.getVisit().getId())
                .orElseThrow(() -> notFound("Không tìm thấy lượt khám."));
        ensureInvoiceNotPaid(visit);
        if (visit.getStatus() != Visit.Status.WAITING_PAYMENT) throw badRequest("Chỉ mở lại hồ sơ đã kết thúc khám.");
        visit.setStatus(Visit.Status.EXAMINING);
        visit.setUpdatedAt(LocalDateTime.now());
        auditService.record("EXAMINATION_REOPENED", "Examination", examinationId, username,
                "Mở lại hồ sơ khi viện phí chưa được duyệt.");
        return detail(examinationId);
    }

    public boolean canReopen(Long examinationId) {
        Examination exam = examinations.findById(examinationId).orElseThrow(() -> notFound("Không tìm thấy hồ sơ khám."));
        return exam.getVisit().getStatus() == Visit.Status.WAITING_PAYMENT
                && invoices.findFirstByVisit_IdOrderByIdDesc(exam.getVisit().getId())
                    .map(invoice -> invoice.getStatus() != Invoice.Status.PAID).orElse(true);
    }

    private void ensureInvoiceNotPaid(Visit visit) {
        if (invoices.findFirstByVisit_IdOrderByIdDesc(visit.getId())
                .map(invoice -> invoice.getStatus() == Invoice.Status.PAID).orElse(false)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Không thể mở hoặc sửa hồ sơ vì viện phí đã được duyệt.");
        }
    }

    private void updateExisting(Examination exam, CompleteRequest request, User doctor, String username) {
        Visit visit = visits.findByIdForUpdate(exam.getVisit().getId())
                .orElseThrow(() -> notFound("Không tìm thấy lượt khám."));
        ensureInvoiceNotPaid(visit);
        ExaminationRoom room = rooms.findById(request.roomId()).filter(r -> r.getStatus() == ExaminationRoom.Status.ACTIVE)
                .orElseThrow(() -> badRequest("Vui lòng chọn phòng khám đang hoạt động."));
        Diagnosis diagnosis = diagnoses.findByIcdCode(request.icdCode().trim().toUpperCase(Locale.ROOT))
                .filter(d -> d.getStatus() == Diagnosis.Status.ACTIVE).orElseThrow(() -> badRequest("Mã ICD không có trong danh mục đang hoạt động."));
        LocalDateTime now = LocalDateTime.now();
        visit.setRoom(room); visit.setReason(request.symptoms()); visit.setUpdatedAt(now); visit.setStatus(Visit.Status.WAITING_PAYMENT);
        exam.setSymptoms(request.symptoms()); exam.setMedicalHistory(request.medicalHistory()); exam.setClinicalNote(request.clinicalNote());
        exam.setExaminationResult(diagnosis.getName()); exam.setAdvice(request.advice()); exam.setDoctor(doctor);
        exam.setStatus(Examination.Status.COMPLETED); exam.setExaminedAt(now); exam.setUpdatedAt(now);
        exam.setPaperRecordStatus(Examination.PaperRecordStatus.PENDING_PRINT);
        examinationDiagnoses.deleteAll(examinationDiagnoses.findByExamination_Id(exam.getId()));
        ExaminationDiagnosis examDiagnosis = new ExaminationDiagnosis(); examDiagnosis.setExamination(exam); examDiagnosis.setDiagnosis(diagnosis); examinationDiagnoses.save(examDiagnosis);
        Prescription prescription = prescriptions.findByExamination_Id(exam.getId()).orElse(null);
        if (prescription != null) {
            prescriptionDetails.deleteAll(prescriptionDetails.findByPrescription_Examination_Id(exam.getId()));
            prescriptions.delete(prescription);
        }
        if (!request.medicines().isEmpty() || !request.supplies().isEmpty()) {
            prescription = new Prescription(); prescription.setPrescriptionCode(prescriptionCode()); prescription.setVisit(visit);
            prescription.setExamination(exam); prescription.setDoctor(doctor); prescription.setPrescriptionDate(now);
            prescription.setStatus(Prescription.Status.PRESCRIBED); prescription.setPaperRecordStatus(Prescription.PaperRecordStatus.PENDING_PRINT);
            prescriptions.save(prescription);
            BigDecimal total = BigDecimal.ZERO;
            for (MedicineRequest item : request.medicines()) total = total.add(addPrescriptionLine(prescription, item, Medicine.InventoryType.MEDICINE));
            for (SupplyRequest item : request.supplies()) total = total.add(addPrescriptionLine(prescription, item.supplyId(), item.quantity(), item.instruction(), Medicine.InventoryType.SUPPLY));
            prescription.setTotalAmount(total);
        }
        if (request.vitalSigns() != null) {
            VitalSign vital = vitalSigns.findByVisit_Id(visit.getId()).orElseGet(VitalSign::new);
            vital.setVisit(visit); vital.setMeasuredBy(doctor); vital.setMeasuredAt(now);
            vital.setBloodPressureSystolic(request.vitalSigns().systolic()); vital.setBloodPressureDiastolic(request.vitalSigns().diastolic());
            vital.setPulse(request.vitalSigns().pulse()); vital.setTemperature(request.vitalSigns().temperature()); vital.setWeight(request.vitalSigns().weight());
            vitalSigns.save(vital);
        } else vitalSigns.findByVisit_Id(visit.getId()).ifPresent(vitalSigns::delete);
        auditService.record("EXAMINATION_UPDATED", "Examination", exam.getId(), username,
                "Cập nhật hồ sơ khám đã mở lại trước khi duyệt viện phí.");
    }

    @Transactional
    public HistoryDetail updatePaperRecordStatus(Long examinationId, Examination.PaperRecordStatus status,
                                                  String username) {
        Examination exam = examinations.findById(examinationId)
                .orElseThrow(() -> notFound("Không tìm thấy hồ sơ khám."));
        if (status == null) throw badRequest("Trạng thái hồ sơ giấy không hợp lệ.");
        if (status.ordinal() < paperStatus(exam).ordinal()) {
            throw badRequest("Trạng thái hồ sơ giấy không được lùi về trước.");
        }
        exam.setPaperRecordStatus(status);
        auditService.record("PAPER_RECORD_STATUS_CHANGED", "Examination", examinationId, username,
                "Cập nhật trạng thái hồ sơ giấy: " + status.name());
        return detail(examinationId);
    }

    private Examination.PaperRecordStatus paperStatus(Examination exam) {
        return exam.getPaperRecordStatus() == null
                ? Examination.PaperRecordStatus.PENDING_PRINT : exam.getPaperRecordStatus();
    }

    private ResponseStatusException badRequest(String message) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, message); }
    private ResponseStatusException notFound(String message) { return new ResponseStatusException(HttpStatus.NOT_FOUND, message); }

    private String prescriptionCode() {
        String alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        StringBuilder suffix = new StringBuilder(7);
        var random = java.util.concurrent.ThreadLocalRandom.current();
        for (int i = 0; i < 7; i++) suffix.append(alphabet.charAt(random.nextInt(alphabet.length())));
        return "TCANG" + suffix + "-C";
    }

    private BigDecimal addPrescriptionLine(Prescription prescription, MedicineRequest request,
                                           Medicine.InventoryType expectedType) {
        Medicine item = medicines.findById(request.medicineId()).filter(m -> m.getStatus() == Medicine.Status.ACTIVE
                        && (m.getInventoryType() == null ? Medicine.InventoryType.MEDICINE : m.getInventoryType()) == expectedType)
                .orElseThrow(() -> badRequest("Thuốc không có trong danh mục đang hoạt động."));
        PrescriptionDetail line = new PrescriptionDetail();
        line.setPrescription(prescription);
        line.setMedicine(item);
        line.setQuantity(request.quantity());
        line.setUnit(item.getUnit());
        line.setDosage(request.dosage());
        line.setFrequency(request.frequency());
        line.setDuration(request.duration());
        line.setRoute(request.route());
        line.setInstruction(request.instruction());
        line.setUnitPrice(item.getPrice());
        line.setTotalPrice(item.getPrice().multiply(BigDecimal.valueOf(request.quantity())));
        prescriptionDetails.save(line);
        return line.getTotalPrice();
    }

    private BigDecimal addPrescriptionLine(Prescription prescription, Long itemId, Integer quantity, String instruction,
                                           Medicine.InventoryType expectedType) {
        Medicine item = medicines.findById(itemId).filter(m -> m.getStatus() == Medicine.Status.ACTIVE
                        && (m.getInventoryType() == null ? Medicine.InventoryType.MEDICINE : m.getInventoryType()) == expectedType)
                .orElseThrow(() -> badRequest(expectedType == Medicine.InventoryType.SUPPLY ? "Vật tư không có trong kho đang hoạt động." : "Thuốc không có trong danh mục đang hoạt động."));
        PrescriptionDetail line = new PrescriptionDetail();
        line.setPrescription(prescription);
        line.setMedicine(item);
        line.setQuantity(quantity);
        line.setUnit(item.getUnit());
        line.setInstruction(instruction);
        line.setUnitPrice(item.getPrice());
        line.setTotalPrice(item.getPrice().multiply(BigDecimal.valueOf(quantity)));
        prescriptionDetails.save(line);
        return line.getTotalPrice();
    }

    public record HistoryRow(Long id, String visitCode, String patientCode, String patientName, String phone,
                             LocalDate visitDate, LocalDateTime examinedAt, String roomName, String doctorName,
                             String status, String result) {}
    public record HistoryPage(List<HistoryRow> items, long total, int totalPages, int page) {}
    public record DiagnosisItem(String code, String name) {}
    public record MedicineItem(Long medicineId, String name, String inventoryType, Integer quantity, String unit,
                               String dosage, String frequency, String duration, String route, String instruction) {}
    public record VitalSummary(BigDecimal systolic, BigDecimal diastolic, Integer pulse, BigDecimal temperature, BigDecimal weight) {}
    public record HistoryDetail(HistoryRow visit, String symptoms, String medicalHistory, String clinicalNote,
                                String result, String advice, List<DiagnosisItem> diagnoses, List<MedicineItem> medicines,
                                VitalSummary vitalSigns, String paperRecordStatus, Long prescriptionId) {}
    public record VitalRequest(@DecimalMin("40") @DecimalMax("300") BigDecimal systolic,
                               @DecimalMin("20") @DecimalMax("200") BigDecimal diastolic,
                               @Min(0) @Max(300) Integer pulse,
                               @DecimalMin("25") @DecimalMax("45") BigDecimal temperature,
                               @DecimalMin("0.1") @DecimalMax("500") BigDecimal weight) {}
    public record MedicineRequest(@NotNull Long medicineId, @NotNull @Min(1) @Max(10000) Integer quantity,
                                  @NotBlank @Size(max = 100) String dosage,
                                  @NotBlank @Size(max = 100) String frequency,
                                  @NotBlank @Size(max = 100) String duration,
                                  @NotBlank @Size(max = 100) String route,
                                  @NotBlank @Size(max = 2000) String instruction) {
        public MedicineRequest {
            dosage = fallback(dosage);
            frequency = fallback(frequency);
            duration = fallback(duration);
            route = fallback(route);
        }

        public MedicineRequest(Long medicineId, Integer quantity, String instruction) {
            this(medicineId, quantity, "—", "—", "—", "—", instruction);
        }

        private static String fallback(String value) {
            return value == null || value.isBlank() ? "—" : value;
        }
    }
    public record SupplyRequest(@NotNull Long supplyId, @NotNull @Min(1) @Max(10000) Integer quantity,
                                @NotBlank @Size(max = 2000) String instruction) {}
    public record CompleteRequest(@NotBlank @Pattern(regexp = "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}") String requestId,
                                  @NotBlank @Size(max = 30) String patientCode, @NotNull Long roomId,
                                  @NotBlank @Size(max = 20) String icdCode,
                                  @Size(max = 10000) String symptoms, @Size(max = 10000) String medicalHistory,
                                  @Size(max = 10000) String clinicalNote, @Size(max = 10000) String advice,
                                  @NotNull @Size(max = 100) List<@NotNull @Valid MedicineRequest> medicines,
                                  @NotNull @Size(max = 100) List<@NotNull @Valid SupplyRequest> supplies,
                                  @Valid VitalRequest vitalSigns) {
        public CompleteRequest {
            medicines = medicines == null ? List.of() : medicines;
            supplies = supplies == null ? List.of() : supplies;
        }

        public CompleteRequest(String requestId, String patientCode, Long roomId, String icdCode,
                               String symptoms, String medicalHistory, String clinicalNote, String advice,
                               List<MedicineRequest> medicines, VitalRequest vitalSigns) {
            this(requestId, patientCode, roomId, icdCode, symptoms, medicalHistory, clinicalNote, advice,
                    medicines, List.of(), vitalSigns);
        }
    }
}
