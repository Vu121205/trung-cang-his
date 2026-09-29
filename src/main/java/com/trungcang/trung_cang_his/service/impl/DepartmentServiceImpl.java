package com.trungcang.trung_cang_his.service.impl;

import com.trungcang.trung_cang_his.domain.Department;
import com.trungcang.trung_cang_his.repository.DepartmentRepository;
import com.trungcang.trung_cang_his.service.DepartmentService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DepartmentServiceImpl implements DepartmentService {

    private final DepartmentRepository departmentRepository;

    public DepartmentServiceImpl(DepartmentRepository departmentRepository) {
        this.departmentRepository = departmentRepository;
    }

    @Override
    public List<Department> getAll() {
        return departmentRepository.findAll();
    }

    @Override
    public Department getById(Long id) {
        return departmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Department not found with id: " + id));
    }

    @Override
    public Department create(Department department) {
        return departmentRepository.save(department);
    }

    @Override
    public Department update(Long id, Department department) {
        Department existing = getById(id);
        existing.setName(department.getName());
        existing.setDescription(department.getDescription());
        existing.setStatus(department.getStatus());
        return departmentRepository.save(existing);
    }

    @Override
    public void delete(Long id) {
        departmentRepository.deleteById(id);
    }
}
