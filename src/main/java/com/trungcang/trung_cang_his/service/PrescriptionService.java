package com.trungcang.trung_cang_his.service;

import com.trungcang.trung_cang_his.domain.Prescription;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface PrescriptionService {
    List<Prescription> getAll();
    Prescription getById(Long id);
    Prescription create(Prescription prescription);
    Prescription update(Long id, Prescription prescription);
    void delete(Long id);
    PrescriptionPrintData getPrintData(Long id);

    record PrescriptionPrintData(Long id, String prescriptionCode, LocalDateTime prescriptionDate,
                                 String facilityName, String facilityAddress, String facilityPhone,
                                 String patientCode, String patientName, LocalDate dateOfBirth,
                                 String gender, String identityNumber, String healthInsuranceNumber,
                                 String address, String phone, BigDecimal weight, String diagnosis,
                                 String advice, String doctorName, String doctorEmployeeCode,
                                 List<PrescriptionPrintLine> lines) {}

    record PrescriptionPrintLine(String medicineName, String activeIngredient, String strength,
                                 String dosageForm, String quantity, String unit, String dosage,
                                 String frequency, String duration, String route, String instruction) {}
}
