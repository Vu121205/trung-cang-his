package com.trungcang.trung_cang_his.service.impl;

import com.trungcang.trung_cang_his.domain.Prescription;
import com.trungcang.trung_cang_his.domain.PrescriptionDetail;
import com.trungcang.trung_cang_his.repository.PrescriptionRepository;
import com.trungcang.trung_cang_his.repository.PrescriptionDetailRepository;
import com.trungcang.trung_cang_his.repository.VitalSignRepository;
import com.trungcang.trung_cang_his.service.PrescriptionService;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class PrescriptionServiceImpl implements PrescriptionService {

    private final PrescriptionRepository prescriptionRepository;
    private final PrescriptionDetailRepository detailRepository;
    private final VitalSignRepository vitalSignRepository;
    @Value("${his.facility.name:Phòng khám Trung Cang}")
    private String facilityName;
    @Value("${his.facility.address:Chưa cấu hình}")
    private String facilityAddress;
    @Value("${his.facility.phone:Chưa cấu hình}")
    private String facilityPhone;

    public PrescriptionServiceImpl(PrescriptionRepository prescriptionRepository,
                                   PrescriptionDetailRepository detailRepository,
                                   VitalSignRepository vitalSignRepository) {
        this.prescriptionRepository = prescriptionRepository;
        this.detailRepository = detailRepository;
        this.vitalSignRepository = vitalSignRepository;
    }

    @Override
    public List<Prescription> getAll() {
        return prescriptionRepository.findAll();
    }

    @Override
    public Prescription getById(Long id) {
        return prescriptionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Prescription not found with id: " + id));
    }

    @Override
    public Prescription create(Prescription prescription) {
        return prescriptionRepository.save(prescription);
    }

    @Override
    public Prescription update(Long id, Prescription prescription) {
        Prescription existing = getById(id);
        existing.setPrescriptionCode(prescription.getPrescriptionCode());
        existing.setStatus(prescription.getStatus());
        existing.setNote(prescription.getNote());
        return prescriptionRepository.save(existing);
    }

    @Override
    public void delete(Long id) {
        Prescription existing = getById(id);
        if (existing.getStatus() == Prescription.Status.DISPENSED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Không thể hủy đơn thuốc đã cấp phát.");
        }
        existing.setStatus(Prescription.Status.CANCELLED);
        prescriptionRepository.save(existing);
    }

    @Override
    @Transactional(readOnly = true)
    public PrescriptionPrintData getPrintData(Long id) {
        Prescription prescription = getById(id);
        var patient = prescription.getVisit().getPatient();
        var weight = vitalSignRepository.findByVisit_Id(prescription.getVisit().getId())
                .map(value -> value.getWeight()).orElse(null);
        var lines = detailRepository.findAllByPrescription_Visit_Id(prescription.getVisit().getId()).stream()
                .filter(line -> line.getPrescription().getId().equals(id))
                .map(this::printLine).toList();
        String diagnosis = prescription.getExamination() == null
                ? prescription.getVisit().getReason() : prescription.getExamination().getExaminationResult();
        return new PrescriptionPrintData(prescription.getId(), prescription.getPrescriptionCode(),
                prescription.getPrescriptionDate(), facilityName, facilityAddress, facilityPhone,
                patient.getPatientCode(), patient.getFullName(),
                patient.getDateOfBirth(), patient.getGender() == null ? null : patient.getGender().name(),
                patient.getIdentityNumber(), patient.getHealthInsuranceNumber(), patient.getAddress(),
                patient.getPhone(), weight, diagnosis, prescription.getNote(),
                prescription.getDoctor().getFullName(), prescription.getDoctor().getEmployeeCode(), lines);
    }

    private PrescriptionPrintLine printLine(PrescriptionDetail line) {
        var medicine = line.getMedicine();
        return new PrescriptionPrintLine(medicine.getName(), medicine.getActiveIngredient(), medicine.getStrength(),
                medicine.getDosageForm(), String.valueOf(line.getQuantity()), line.getUnit(), line.getDosage(),
                line.getFrequency(), line.getDuration(), line.getRoute(), line.getInstruction());
    }
}
