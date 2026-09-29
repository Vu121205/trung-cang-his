package com.trungcang.trung_cang_his.service;

import com.trungcang.trung_cang_his.domain.Role;
import com.trungcang.trung_cang_his.domain.User;
import com.trungcang.trung_cang_his.repository.RoleRepository;
import com.trungcang.trung_cang_his.repository.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class EmployeeService {
    private static final List<String> ROLE_CODES = List.of("ADMIN", "DOCTOR", "RECEPTION", "PHARMACIST");
    private final UserRepository users;
    private final RoleRepository roles;
    private final PasswordEncoder passwords;

    public EmployeeService(UserRepository users, RoleRepository roles, PasswordEncoder passwords) {
        this.users = users;
        this.roles = roles;
        this.passwords = passwords;
    }

    @Transactional(readOnly = true)
    public List<EmployeeView> list() {
        return users.findAllByDeletedFalseOrderByFullNameAsc().stream().map(EmployeeView::from).toList();
    }

    @Transactional(readOnly = true)
    public List<RoleView> roles() {
        return roles.findAll().stream().filter(r -> ROLE_CODES.contains(r.getCode()))
                .map(r -> new RoleView(r.getCode(), r.getName())).toList();
    }

    public EmployeeView create(CreateRequest request, String actor) {
        lockAndCheckAdmin(actor);
        String username = request.username().trim();
        if (users.existsByUsernameIgnoreCase(username)) throw conflict("Tên đăng nhập đã được sử dụng.");
        User staff = new User();
        staff.setUsername(username);
        staff.setPassword(encodePassword(request.password()));
        staff.setCreatedAt(LocalDateTime.now());
        apply(staff, request.profile());
        return EmployeeView.from(users.saveAndFlush(staff));
    }

    public EmployeeView update(Long id, ProfileRequest request, String actor) {
        lockAndCheckAdmin(actor);
        User staff = require(id);
        boolean accessChanged = !staff.getRole().getCode().equals(request.roleCode()) || staff.getStatus() != request.status();
        if (staff.getUsername().equalsIgnoreCase(actor)
                && (!"ADMIN".equals(request.roleCode()) || request.status() != User.Status.ACTIVE)) {
            throw conflict("Bạn không thể tự bỏ quyền Admin hoặc khóa tài khoản đang đăng nhập.");
        }
        apply(staff, request);
        if (accessChanged) staff.setSecurityVersion(staff.getSecurityVersion() + 1);
        return EmployeeView.from(users.saveAndFlush(staff));
    }

    public void changePassword(Long id, String password, String actor) {
        lockAndCheckAdmin(actor);
        User staff = require(id);
        staff.setPassword(encodePassword(password));
        staff.setSecurityVersion(staff.getSecurityVersion() + 1);
        staff.setUpdatedAt(LocalDateTime.now());
    }

    public void delete(Long id, String actor) {
        lockAndCheckAdmin(actor);
        User staff = require(id);
        if (staff.getUsername().equalsIgnoreCase(actor)) throw conflict("Bạn không thể xóa tài khoản đang đăng nhập.");
        // Keep the identifier for existing visits, examinations and invoices.
        staff.setDeleted(true);
        staff.setStatus(User.Status.INACTIVE);
        staff.setSecurityVersion(staff.getSecurityVersion() + 1);
        staff.setUpdatedAt(LocalDateTime.now());
    }

    private void lockAndCheckAdmin(String actor) {
        // Serialize administrative changes so concurrent admins cannot disable each other.
        User admin = users.lockForAdministration().stream()
                .filter(u -> u.getUsername().equalsIgnoreCase(actor)).findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN));
        if (admin.isDeleted() || admin.getStatus() != User.Status.ACTIVE || !"ADMIN".equals(admin.getRole().getCode()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tài khoản không còn quyền quản lý nhân viên.");
    }

    private User require(Long id) {
        return users.findById(id).filter(u -> !u.isDeleted())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy nhân viên."));
    }

    private void apply(User staff, ProfileRequest request) {
        String code = optional(request.employeeCode());
        if (code != null && users.existsByEmployeeCodeIgnoreCaseAndIdNot(code, staff.getId() == null ? -1L : staff.getId()))
            throw conflict("Mã nhân viên đã được sử dụng.");
        if (!ROLE_CODES.contains(request.roleCode()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nhóm quyền không hợp lệ.");
        Role role = roles.findByCode(request.roleCode())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không tìm thấy nhóm quyền."));
        staff.setFullName(request.fullName().trim());
        staff.setEmployeeCode(code);
        staff.setPhone(optional(request.phone()));
        staff.setEmail(optional(request.email()));
        staff.setDateOfBirth(request.dateOfBirth());
        staff.setGender(optional(request.gender()));
        staff.setAddress(optional(request.address()));
        staff.setIdentityNumber(optional(request.identityNumber()));
        staff.setDepartment(optional(request.department()));
        staff.setJobTitle(optional(request.jobTitle()));
        staff.setSpecialization(optional(request.specialization()));
        staff.setRole(role);
        staff.setStatus(request.status());
        staff.setUpdatedAt(LocalDateTime.now());
    }

    private String encodePassword(String password) {
        if (password == null || password.isBlank() || password.length() < 8
                || password.getBytes(StandardCharsets.UTF_8).length > 72)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mật khẩu cần ít nhất 8 ký tự và tối đa 72 byte UTF-8.");
        return passwords.encode(password);
    }

    private static String optional(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    private static ResponseStatusException conflict(String message) { return new ResponseStatusException(HttpStatus.CONFLICT, message); }

    public record ProfileRequest(
            @NotBlank @Size(max=150) String fullName,
            @Size(max=50) String employeeCode,
            @Size(max=20) @Pattern(regexp="[+0-9() .-]*") String phone,
            @Email @Size(max=150) String email,
            @PastOrPresent LocalDate dateOfBirth,
            @Pattern(regexp="MALE|FEMALE|OTHER|^$") String gender,
            @Size(max=500) String address,
            @Size(max=20) String identityNumber,
            @Size(max=150) String department,
            @Size(max=150) String jobTitle,
            @Size(max=150) String specialization,
            @NotBlank String roleCode,
            @NotNull User.Status status) {}

    public record CreateRequest(
            @NotBlank @Size(max=100) @Pattern(regexp="[a-zA-Z0-9._-]+") String username,
            @NotBlank @Size(min=8, max=72) String password,
            @NotNull @Valid ProfileRequest profile) {
        @Override public String toString() { return "CreateRequest[password=REDACTED]"; }
    }
    public record PasswordRequest(@NotBlank @Size(min=8, max=72) String password) {
        @Override public String toString() { return "PasswordRequest[password=REDACTED]"; }
    }
    public record RoleView(String code, String name) {}
    public record EmployeeView(Long id, String username, String fullName, String employeeCode, String phone,
            String email, LocalDate dateOfBirth, String gender, String address, String identityNumber,
            String department, String jobTitle, String specialization, String roleCode, String roleName,
            User.Status status) {
        static EmployeeView from(User u) {
            return new EmployeeView(u.getId(), u.getUsername(), u.getFullName(), u.getEmployeeCode(), u.getPhone(),
                    u.getEmail(), u.getDateOfBirth(), u.getGender(), u.getAddress(), u.getIdentityNumber(),
                    u.getDepartment(), u.getJobTitle(), u.getSpecialization(), u.getRole().getCode(),
                    u.getRole().getName(), u.getStatus());
        }
    }
}
