package com.trungcang.trung_cang_his.service;

import com.trungcang.trung_cang_his.domain.Department;

import java.util.List;

public interface DepartmentService {
    List<Department> getAll();
    Department getById(Long id);
    Department create(Department department);
    Department update(Long id, Department department);
    void delete(Long id);
}
