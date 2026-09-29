package com.trungcang.trung_cang_his.service.impl;

import com.trungcang.trung_cang_his.domain.Medicine;
import com.trungcang.trung_cang_his.repository.MedicineRepository;
import com.trungcang.trung_cang_his.service.MedicineService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MedicineServiceImpl implements MedicineService {

    private final MedicineRepository medicineRepository;

    public MedicineServiceImpl(MedicineRepository medicineRepository) {
        this.medicineRepository = medicineRepository;
    }

    @Override
    public List<Medicine> getAll() {
        return medicineRepository.findAll();
    }

    @Override
    public Medicine getById(Long id) {
        return medicineRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Medicine not found with id: " + id));
    }

    @Override
    public Medicine create(Medicine medicine) {
        return medicineRepository.save(medicine);
    }

    @Override
    public Medicine update(Long id, Medicine medicine) {
        Medicine existing = getById(id);
        existing.setName(medicine.getName());
        existing.setPrice(medicine.getPrice());
        existing.setStatus(medicine.getStatus());
        return medicineRepository.save(existing);
    }

    @Override
    public void delete(Long id) {
        medicineRepository.deleteById(id);
    }
}
