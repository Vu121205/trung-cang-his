package com.trungcang.trung_cang_his.config;

import com.trungcang.trung_cang_his.domain.Role;
import com.trungcang.trung_cang_his.domain.Patient;
import com.trungcang.trung_cang_his.domain.User;
import com.trungcang.trung_cang_his.repository.PatientRepository;
import com.trungcang.trung_cang_his.repository.RoleRepository;
import com.trungcang.trung_cang_his.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Configuration
@Profile("!prod")
public class DatabaseSeeder {

    @Bean
    CommandLineRunner seedUsers(RoleRepository roleRepository,
                                UserRepository userRepository,
                                PatientRepository patientRepository,
                                PasswordEncoder passwordEncoder,
                                PlatformTransactionManager transactionManager) {
        return args -> {
            new TransactionTemplate(transactionManager).executeWithoutResult(transaction -> {
                Role adminRole = ensureRole(roleRepository, "ADMIN", "Admin");
                Role doctorRole = ensureRole(roleRepository, "DOCTOR", "Bác Sĩ");
                Role receptionRole = ensureRole(roleRepository, "RECEPTION", "Lễ Tân");
                Role pharmacistRole = ensureRole(roleRepository, "PHARMACIST", "Dược Sĩ");
                migrateLegacyRoles(roleRepository, userRepository, Map.of(
                        "ADMIN", adminRole, "DOCTOR", doctorRole,
                        "RECEPTION", receptionRole, "PHARMACIST", pharmacistRole,
                        "NURSE", receptionRole, "ACCOUNTANT", pharmacistRole,
                        "RECEPTIONIST", receptionRole, "CASHIER", pharmacistRole));

                List<UserSeed> staff = List.of(
                        new UserSeed("admin", "pkdktc68@", "Admin", adminRole),
                        new UserSeed("bacsi", "pkdktc68@", "bác sĩ", doctorRole),
                        new UserSeed("duocsi", "pkdktc68@", "dược sĩ", pharmacistRole),
                        new UserSeed("letan", "pkdktc68@", "lễ tân", receptionRole),
                        new UserSeed("HTGBS", "pkdktc68@", "Huỳnh Trường Giang", doctorRole),
                        new UserSeed("LHQBS", "pkdktc68@", "Lê Hoàng Qui", doctorRole),
                        new UserSeed("LVKDD", "pkdktc68@", "Lê Văn Kha", receptionRole),
                        new UserSeed("LTDBS", "pkdktc68@", "Lý Thành Du", doctorRole),
                        new UserSeed("LQDBS", "pkdktc68@", "Lưu Quốc Duy", doctorRole),
                        new UserSeed("NTSBS", "pkdktc68@", "Nguyễn Thanh Sang", doctorRole),
                        new UserSeed("LNDHKT", "pkdktc68@", "Lê Nguyên Đức Hạnh", pharmacistRole),
                        new UserSeed("TLPVDD", "pkdktc68@", "Trần Lệ Phương Vy", receptionRole),
                        new UserSeed("NBTBS", "pkdktc68@", "Nguyễn Bảo Trân", doctorRole),
                        new UserSeed("NTNTRBS", "pkdktc68@", "Nguyễn Thị Ngọc Trinh", doctorRole),
                        new UserSeed("NTTTBS", "pkdktc68@", "Nguyễn Thị Thảo Tiên", doctorRole),
                        new UserSeed("HTBQDD", "pkdktc68@", "Huỳnh Thị Bảo Quỳnh", receptionRole),
                        new UserSeed("NTNBS", "pkdktc68@", "Nguyễn Thành Nam", doctorRole)
                );

                for (UserSeed userSeed : staff) {
                    // Administrative changes (including deleted accounts) must survive restarts.
                    if (userRepository.existsByUsername(userSeed.username)) continue;
                    User user = new User();
                    user.setUsername(userSeed.username);
                    user.setPassword(passwordEncoder.encode(userSeed.password));
                    user.setFullName(userSeed.fullName);
                    user.setRole(userSeed.role);
                    user.setStatus(User.Status.ACTIVE);
                    if (user.getCreatedAt() == null) user.setCreatedAt(LocalDateTime.now());
                    user.setUpdatedAt(LocalDateTime.now());
                    userRepository.save(user);
                }

            });
            seedDemoPatients(patientRepository);
        };
    }

    private void seedDemoPatients(PatientRepository patientRepository) {
        if (patientRepository.count() > 0) {
            return;
        }

        List<PatientSeed> patients = List.of(
                new PatientSeed("Nguyễn Minh Anh", "0901000001", "MALE", 1990),
                new PatientSeed("Trần Thị Bích", "0901000002", "FEMALE", 1988),
                new PatientSeed("Lê Hoàng Nam", "0901000003", "MALE", 1975),
                new PatientSeed("Phạm Ngọc Lan", "0901000004", "FEMALE", 1995),
                new PatientSeed("Võ Đức Huy", "0901000005", "MALE", 1982),
                new PatientSeed("Đặng Thu Hà", "0901000006", "FEMALE", 1992),
                new PatientSeed("Bùi Quang Vinh", "0901000007", "MALE", 1968),
                new PatientSeed("Ngô Thị Mai", "0901000008", "FEMALE", 1979),
                new PatientSeed("Đỗ Minh Khang", "0901000009", "MALE", 2001),
                new PatientSeed("Huỳnh Kim Chi", "0901000010", "FEMALE", 1998),
                new PatientSeed("Phan Văn Toàn", "0901000011", "MALE", 1987),
                new PatientSeed("Mai Thị Hương", "0901000012", "FEMALE", 1972),
                new PatientSeed("Dương Quốc Bảo", "0901000013", "MALE", 1993),
                new PatientSeed("Vũ Ngọc Ánh", "0901000014", "FEMALE", 1985),
                new PatientSeed("Nguyễn Thành Đạt", "0901000015", "MALE", 2004),
                new PatientSeed("Lý Thùy Dương", "0901000016", "FEMALE", 1991),
                new PatientSeed("Trương Công Minh", "0901000017", "MALE", 1965),
                new PatientSeed("Cao Thị Yến", "0901000018", "FEMALE", 1980),
                new PatientSeed("Hoàng Gia Phúc", "0901000019", "MALE", 1999),
                new PatientSeed("Tạ Ngọc Trinh", "0901000020", "FEMALE", 1976)
        );

        LocalDateTime now = LocalDateTime.now();
        for (int index = 0; index < patients.size(); index++) {
            PatientSeed seed = patients.get(index);
            Patient patient = new Patient();
            patient.setPatientCode("BN" + String.format("%05d", 20001 + index));
            patient.setFullName(seed.fullName);
            patient.setPhone(seed.phone);
            patient.setGender(Patient.Gender.valueOf(seed.gender));
            patient.setDateOfBirth(LocalDate.of(seed.birthYear, 1, 1));
            patient.setAddress("Phường trung tâm, Thành phố Hồ Chí Minh");
            patient.setStatus(Patient.Status.ACTIVE);
            patient.setCreatedAt(now);
            patient.setUpdatedAt(now);
            patientRepository.save(patient);
        }
    }

    private Role ensureRole(RoleRepository roleRepository, String code, String name) {
        Role role = roleRepository.findByCode(code).orElseGet(Role::new);
        role.setCode(code);
        role.setName(name);
        if (role.getCreatedAt() == null) role.setCreatedAt(LocalDateTime.now());
        return roleRepository.save(role);
    }

    private void migrateLegacyRoles(RoleRepository roles, UserRepository users, Map<String, Role> replacements) {
        for (Role legacy : roles.findAll()) {
            String code = legacy.getCode();
            while (code.startsWith("ROLE_")) code = code.substring(5);
            Role replacement = replacements.get(code);
            if (replacement == null) {
                // Remove obsolete, unused groups without assigning arbitrary rights to staff.
                if (!users.existsByRole_Id(legacy.getId())) roles.delete(legacy);
                continue;
            }
            if (replacement.getId().equals(legacy.getId())) continue;
            for (User user : users.findAllByRole_Id(legacy.getId())) {
                user.setRole(replacement);
                user.setUpdatedAt(LocalDateTime.now());
                users.save(user);
            }
            users.flush();
            roles.delete(legacy);
        }
    }

    private record UserSeed(String username, String password, String fullName, Role role) {
    }

    private record PatientSeed(String fullName, String phone, String gender, int birthYear) {
    }
}
