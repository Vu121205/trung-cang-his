package com.trungcang.trung_cang_his;

import com.trungcang.trung_cang_his.domain.*;
import com.trungcang.trung_cang_his.repository.*;
import com.trungcang.trung_cang_his.service.ExaminationHistoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ExaminationHistoryTests {
    @Autowired MockMvc mvc;
    @Autowired ExaminationHistoryService history;
    @Autowired PatientRepository patients;
    @Autowired DepartmentRepository departments;
    @Autowired ExaminationRoomRepository rooms;
        @Autowired MedicalServiceRepository medicalServices;
    @Autowired DiagnosisRepository diagnoses;
    @Autowired MedicineRepository medicines;
    @Autowired ExaminationRepository examinations;
    @Autowired VisitRepository visits;
        @Autowired InvoiceRepository invoices;
        @Autowired PrescriptionRepository prescriptions;
        @Autowired MedicineBatchRepository batches;
    private Patient patient;
    private ExaminationRoom room;
    private Diagnosis diagnosis;
    private Medicine medicine;

    @BeforeEach
    void fixture() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        patient = new Patient(); patient.setPatientCode("HIS-" + suffix);
        patient.setFullName("Nguyễn Lịch Sử " + suffix); patient.setPhone("0901234567");
        patients.save(patient);
        Department department = new Department(); department.setCode("H-" + suffix); department.setName("Khoa khám thử");
        departments.save(department);
        room = new ExaminationRoom(); room.setCode("PK-H-" + suffix + "-01"); room.setName("Phòng thử " + suffix); room.setDepartment(department);
        rooms.save(room);
        MedicalService examinationService = new MedicalService(); examinationService.setCode("KB-H-" + suffix);
        examinationService.setName("Khám thử " + suffix); examinationService.setType(MedicalService.ServiceType.EXAM);
        examinationService.setPrice(new BigDecimal("150000")); medicalServices.save(examinationService);
        diagnosis = new Diagnosis(); diagnosis.setIcdCode("T-" + suffix.toUpperCase(java.util.Locale.ROOT)); diagnosis.setName("Chẩn đoán thử"); diagnoses.save(diagnosis);
        medicine = new Medicine(); medicine.setCode("M-" + suffix); medicine.setName("Thuốc thử"); medicine.setUnit("Viên");
        medicine.setPrice(new BigDecimal("1500")); medicines.save(medicine);
    }

    private ExaminationHistoryService.CompleteRequest request(String key, Long medicineId) {
        return new ExaminationHistoryService.CompleteRequest(key, patient.getPatientCode(), room.getId(), diagnosis.getIcdCode(),
                "Triệu chứng thử", "Tiền sử thử", "Chỉ định thử", "Dặn dò thử",
                List.of(new ExaminationHistoryService.MedicineRequest(medicineId, 2, "Cách dùng thử")),
                new ExaminationHistoryService.VitalRequest(new BigDecimal("120"), new BigDecimal("80"), 75,
                        new BigDecimal("36.5"), new BigDecimal("60")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "DOCTOR", "RECEPTION", "PHARMACIST"})
    void allGroupsCanReadHistoryWithoutExposingAccountSecrets(String role) throws Exception {
        var saved = history.complete(request(UUID.randomUUID().toString(), medicine.getId()), "bacsi");
        mvc.perform(get("/examination-history").with(user("staff").roles(role))).andExpect(status().isOk())
                .andExpect(content().string(containsString("historyFilters")));
        mvc.perform(get("/api/examination-history").param("q", patient.getPatientCode()).with(user("staff").roles(role)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].patientCode").value(patient.getPatientCode()))
                .andExpect(content().string(not(containsString("password"))));
        mvc.perform(get("/api/examination-history/" + saved.visit().id()).with(user("staff").roles(role)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.symptoms").value("Triệu chứng thử"))
                .andExpect(jsonPath("$.diagnoses[0].code").value(diagnosis.getIcdCode()))
                .andExpect(jsonPath("$.medicines[0].quantity").value(2))
                .andExpect(jsonPath("$.vitalSigns.systolic").value(120))
                .andExpect(jsonPath("$.vitalSigns.weight").value(60))
                .andExpect(content().string(not(containsString("password"))));
    }

    @Test
    void repeatSubmissionIsIdempotentButNextVisitIsSeparate() {
        var request = request(UUID.randomUUID().toString(), medicine.getId());
        var first = history.complete(request, "bacsi");
        var retry = history.complete(request, "bacsi");
        assertThat(retry.visit().id()).isEqualTo(first.visit().id());
        history.complete(request(UUID.randomUUID().toString(), medicine.getId()), "bacsi");
        assertThat(history.search(patient.getPatientCode(), null, null, 0, 20).total()).isEqualTo(2);
    }

    @Test
    void invalidMedicineRollsBackWholeExamination() {
        long examCount = examinations.count();
        long visitCount = visits.count();
        assertThatThrownBy(() -> history.complete(request(UUID.randomUUID().toString(), Long.MAX_VALUE), "bacsi"))
                .isInstanceOf(ResponseStatusException.class);
        assertThat(examinations.count()).isEqualTo(examCount);
        assertThat(visits.count()).isEqualTo(visitCount);
    }

        @Test
        void vitalSignsOutsideClinicalRangeAreRejected() throws Exception {
                long visitCount = visits.count();
                String json = """
                                {"requestId":"%s","patientCode":"%s","roomId":%d,"icdCode":"%s",
                                 "symptoms":"Test","medicines":[],"vitalSigns":{"systolic":400,"diastolic":80}}
                                """.formatted(UUID.randomUUID(), patient.getPatientCode(), room.getId(), diagnosis.getIcdCode());
                mvc.perform(post("/api/examinations/complete").with(user("bacsi").roles("DOCTOR"))
                                                .contentType(MediaType.APPLICATION_JSON).content(json))
                                .andExpect(status().isBadRequest());
                assertThat(visits.count()).isEqualTo(visitCount);
        }

    @Test
    void filteringPaginationAndInputErrors() throws Exception {
        history.complete(request(UUID.randomUUID().toString(), medicine.getId()), "bacsi");
        history.complete(request(UUID.randomUUID().toString(), medicine.getId()), "bacsi");
        var result = history.search(patient.getPatientCode(), LocalDate.now(), LocalDate.now(), 0, 1);
        assertThat(result.total()).isEqualTo(2);
        assertThat(result.totalPages()).isEqualTo(2);
        assertThat(result.items()).hasSize(1);
        assertThat(history.search(patient.getPatientCode(), LocalDate.now().plusDays(1), null, 0, 20).total()).isZero();
        mvc.perform(get("/api/examination-history").param("from", "2026-09-30").param("to", "2026-09-01")
                .with(user("staff").roles("RECEPTION"))).andExpect(status().isBadRequest());
        mvc.perform(get("/api/examination-history").param("from", "not-a-date")
                .with(user("staff").roles("RECEPTION"))).andExpect(status().isBadRequest());
        mvc.perform(get("/api/examination-history").param("size", "1000")
                .with(user("staff").roles("RECEPTION"))).andExpect(status().isBadRequest());
        mvc.perform(get("/api/examination-history/9223372036854775807").with(user("staff").roles("RECEPTION")))
                .andExpect(status().isNotFound());
    }

    @Test
    void onlyClinicalStaffCanCompleteAndAnonymousCannotRead() throws Exception {
        mvc.perform(get("/api/examination-history")).andExpect(status().isUnauthorized());
        mvc.perform(get("/examination-history")).andExpect(status().is3xxRedirection());
        for (String role : List.of("RECEPTION", "PHARMACIST")) {
            mvc.perform(post("/api/examinations/complete").with(user("staff").roles(role))
                    .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isForbidden());
        }
        mvc.perform(post("/api/examinations/complete").with(user("bacsi").roles("DOCTOR"))
                .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isBadRequest());
        String json = """
                {"requestId":"%s","patientCode":"%s","roomId":%d,"icdCode":"%s",
                 "symptoms":"Test","medicines":[{"medicineId":%d,"quantity":2,"instruction":"Test"}]}
                """.formatted(UUID.randomUUID(), patient.getPatientCode(), room.getId(), diagnosis.getIcdCode(), medicine.getId());
        mvc.perform(post("/api/examinations/complete").with(user("bacsi").roles("DOCTOR"))
                .contentType(MediaType.APPLICATION_JSON).content(json)).andExpect(status().isOk())
                .andExpect(jsonPath("$.visit.doctorName").value("bác sĩ"));
    }

    @Test
    void paymentMustPrecedeDispensingAndDispensingConsumesStockOnce() throws Exception {
        String requestId = UUID.randomUUID().toString();
        history.complete(request(requestId, medicine.getId()), "bacsi");
        Visit visit = visits.findByVisitCode("KB-" + requestId).orElseThrow();
        Prescription prescription = prescriptions.findAllByVisit_Id(visit.getId()).getFirst();

        mvc.perform(get("/api/workflow/billing").with(user("cashier").roles("DOCTOR")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/workflow/dispensing").with(user("pharmacist").roles("PHARMACIST")))
                .andExpect(status().isOk()).andExpect(jsonPath("$[?(@.prescriptionId == " + prescription.getId() + ")]").doesNotExist());
        mvc.perform(post("/api/workflow/dispensing/" + prescription.getId() + "/confirm")
                        .with(user("pharmacist").roles("PHARMACIST")))
                .andExpect(status().isConflict());

        var stock = new MedicineBatch();
        stock.setMedicine(medicine);
        stock.setBatchNumber("B-" + requestId);
        stock.setExpiryDate(LocalDate.now().plusMonths(2));
        stock.setQuantity(8);
        stock.setUnitPrice(medicine.getPrice());
        batches.save(stock);

        mvc.perform(post("/api/workflow/billing/" + visit.getId() + "/pay")
                        .with(user("duocsi").roles("PHARMACIST"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"method\":\"BANK_TRANSFER\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAmount").value(153000))
                .andExpect(jsonPath("$.method").value("BANK_TRANSFER"))
                .andExpect(jsonPath("$.status").value("PAID"));
        mvc.perform(get("/api/workflow/dispensing").with(user("pharmacist").roles("PHARMACIST")))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].prescriptionCode").value(prescription.getPrescriptionCode()))
                .andExpect(jsonPath("$[0].lines[0].stock").value(8));

        mvc.perform(post("/api/workflow/dispensing/" + prescription.getId() + "/confirm")
                        .with(user("pharmacist").roles("PHARMACIST")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.prescriptionStatus").value("DISPENSED"))
                .andExpect(jsonPath("$.visitStatus").value("COMPLETED"));
        assertThat(batches.findById(stock.getId()).orElseThrow().getQuantity()).isEqualTo(6);
        assertThat(invoices.findFirstByVisit_IdOrderByIdDesc(visit.getId()).orElseThrow().getPaidAmount())
                .isEqualByComparingTo("153000");
        mvc.perform(post("/api/workflow/dispensing/" + prescription.getId() + "/confirm")
                        .with(user("pharmacist").roles("PHARMACIST")))
                .andExpect(status().isConflict());
    }
}
