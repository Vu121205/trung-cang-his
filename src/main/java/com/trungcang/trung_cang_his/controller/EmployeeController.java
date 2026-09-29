package com.trungcang.trung_cang_his.controller;

import com.trungcang.trung_cang_his.service.EmployeeService;
import com.trungcang.trung_cang_his.service.EmployeeService.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/employees")
public class EmployeeController {
    private final EmployeeService employees;
    public EmployeeController(EmployeeService employees) { this.employees = employees; }

    @GetMapping
    public List<EmployeeView> list() { return employees.list(); }

    @GetMapping("/roles")
    public List<RoleView> roles() { return employees.roles(); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EmployeeView create(@Valid @RequestBody CreateRequest request, Authentication auth) {
        return employees.create(request, auth.getName());
    }

    @PutMapping("/{id}")
    public EmployeeView update(@PathVariable Long id, @Valid @RequestBody ProfileRequest request, Authentication auth) {
        return employees.update(id, request, auth.getName());
    }

    @PutMapping("/{id}/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void password(@PathVariable Long id, @Valid @RequestBody PasswordRequest request, Authentication auth) {
        employees.changePassword(id, request.password(), auth.getName());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, Authentication auth) { employees.delete(id, auth.getName()); }
}
