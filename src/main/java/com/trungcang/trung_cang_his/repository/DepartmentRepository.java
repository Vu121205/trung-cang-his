package com.trungcang.trung_cang_his.repository;

import com.trungcang.trung_cang_his.domain.Department;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DepartmentRepository extends JpaRepository<Department, Long> {
}
