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
                .map(item -> new MedicineItem(item.getMedicine().getName(), item.getQuantity(), item.getUnit(), item.getInstruction())).toList();
        VitalSummary vitalSummary = vitalSigns.findByVisit_Id(exam.getVisit().getId())
            .map(sign -> new VitalSummary(sign.getBloodPressureSystolic(), sign.getBloodPressureDiastolic(), sign.getPulse(),
                sign.getTemperature(), sign.getWeight())).orElse(null);
        return new HistoryDetail(summary(exam), exam.getSymptoms(), exam.getMedicalHistory(),
            exam.getClinicalNote(), exam.getExaminationResult(), exam.getAdvice(), diagnosisItems, medicineItems, vitalSummary);
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
        if (!request.medicines().isEmpty()) {
            Prescription prescription = new Prescription();
            prescription.setPrescriptionCode("DT-" + request.requestId());
            prescription.setVisit(visit);
            prescription.setExamination(exam);
            prescription.setDoctor(doctor);
            prescription.setPrescriptionDate(now);
            prescription.setStatus(Prescription.Status.PRESCRIBED);
            prescriptions.save(prescription);
            BigDecimal total = BigDecimal.ZERO;
            for (MedicineRequest item : request.medicines()) {
                Medicine medicine = medicines.findById(item.medicineId()).filter(m -> m.getStatus() == Medicine.Status.ACTIVE)
                        .orElseThrow(() -> badRequest("Thuốc không có trong danh mục đang hoạt động."));
                PrescriptionDetail line = new PrescriptionDetail();
                line.setPrescription(prescription);
                line.setMedicine(medicine);
                line.setQuantity(item.quantity());
                line.setUnit(medicine.getUnit());
                line.setInstruction(item.instruction());
                line.setUnitPrice(medicine.getPrice());
                line.setTotalPrice(medicine.getPrice().multiply(BigDecimal.valueOf(item.quantity())));
                prescriptionDetails.save(line);
                total = total.add(line.getTotalPrice());
            }
            prescription.setTotalAmount(total);
        }
        return detail(exam.getId());
    }

    private ResponseStatusException badRequest(String message) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, message); }
    private ResponseStatusException notFound(String message) { return new ResponseStatusException(HttpStatus.NOT_FOUND, message); }

    public record HistoryRow(Long id, String visitCode, String patientCode, String patientName, String phone,
                             LocalDate visitDate, LocalDateTime examinedAt, String roomName, String doctorName,
                             String status, String result) {}
    public record HistoryPage(List<HistoryRow> items, long total, int totalPages, int page) {}
    public record DiagnosisItem(String code, String name) {}
    public record MedicineItem(String name, Integer quantity, String unit, String instruction) {}
    public record VitalSummary(BigDecimal systolic, BigDecimal diastolic, Integer pulse, BigDecimal temperature, BigDecimal weight) {}
    public record HistoryDetail(HistoryRow visit, String symptoms, String medicalHistory, String clinicalNote,
                                String result, String advice, List<DiagnosisItem> diagnoses, List<MedicineItem> medicines,
                                VitalSummary vitalSigns) {}
    public record VitalRequest(@DecimalMin("40") @DecimalMax("300") BigDecimal systolic,
                               @DecimalMin("20") @DecimalMax("200") BigDecimal diastolic,
                               @Min(0) @Max(300) Integer pulse,
                               @DecimalMin("25") @DecimalMax("45") BigDecimal temperature,
                               @DecimalMin("0.1") @DecimalMax("500") BigDecimal weight) {}
    public record MedicineRequest(@NotNull Long medicineId, @NotNull @Min(1) @Max(10000) Integer quantity,
                                  @NotBlank @Size(max = 2000) String instruction) {}
    public record CompleteRequest(@NotBlank @Pattern(regexp = "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}") String requestId,
                                  @NotBlank @Size(max = 30) String patientCode, @NotNull Long roomId,
                                  @NotBlank @Size(max = 20) String icdCode,
                                  @Size(max = 10000) String symptoms, @Size(max = 10000) String medicalHistory,
                                  @Size(max = 10000) String clinicalNote, @Size(max = 10000) String advice,
                                  @NotNull @Size(max = 100) List<@NotNull @Valid MedicineRequest> medicines,
                                  @Valid VitalRequest vitalSigns) {}
}
