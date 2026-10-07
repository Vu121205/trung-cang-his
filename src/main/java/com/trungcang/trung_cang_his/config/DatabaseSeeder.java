package com.trungcang.trung_cang_his.config;

import com.trungcang.trung_cang_his.domain.Role;
import com.trungcang.trung_cang_his.domain.Patient;
import com.trungcang.trung_cang_his.domain.User;
import com.trungcang.trung_cang_his.repository.PatientRepository;
import com.trungcang.trung_cang_his.repository.RoleRepository;
import com.trungcang.trung_cang_his.repository.UserRepository;
import com.trungcang.trung_cang_his.repository.MedicineRepository;
import com.trungcang.trung_cang_his.repository.MedicineBatchRepository;
import com.trungcang.trung_cang_his.domain.Medicine;
import com.trungcang.trung_cang_his.domain.MedicineBatch;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.math.BigDecimal;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Configuration
@Profile("!prod")
public class DatabaseSeeder {

    @Bean
    CommandLineRunner seedUsers(RoleRepository roleRepository,
                                UserRepository userRepository,
                                PatientRepository patientRepository,
                                MedicineRepository medicineRepository,
                                MedicineBatchRepository batchRepository,
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
            seedMedicines(medicineRepository, batchRepository);
            seedDemoPatients(patientRepository);
        };
    }

    private void seedMedicines(MedicineRepository medicines, MedicineBatchRepository batches) {
        record MedicineSeed(String code, String name, String ingredient, String strength, String form,
                            String unit, BigDecimal price, int minStock, int quantity, Medicine.InventoryType inventoryType) {}
        List<MedicineSeed> catalog = List.of(
                new MedicineSeed("TD001", "Paracetamol 500 mg", "Paracetamol", "500 mg", "Viên nén", "Viên", new BigDecimal("1500"), 100, 1000, Medicine.InventoryType.MEDICINE),
                new MedicineSeed("TD004", "Ibuprofen 200 mg", "Ibuprofen", "200 mg", "Viên nén", "Viên", new BigDecimal("2000"), 100, 1000, Medicine.InventoryType.MEDICINE),
                new MedicineSeed("TD009", "Loratadine 10 mg", "Loratadine", "10 mg", "Viên nén", "Viên", new BigDecimal("1200"), 100, 1000, Medicine.InventoryType.MEDICINE),
                new MedicineSeed("TD017", "Amoxicillin 500 mg", "Amoxicillin", "500 mg", "Viên nang", "Viên", new BigDecimal("2500"), 100, 1000, Medicine.InventoryType.MEDICINE),
                new MedicineSeed("TD023", "Cefalexin 500 mg", "Cefalexin", "500 mg", "Viên nang", "Viên", new BigDecimal("3000"), 100, 1000, Medicine.InventoryType.MEDICINE),
                new MedicineSeed("TD029", "Azithromycin 500 mg", "Azithromycin", "500 mg", "Viên nén", "Viên", new BigDecimal("4500"), 50, 500, Medicine.InventoryType.MEDICINE),
                new MedicineSeed("TD040", "Omeprazole 20 mg", "Omeprazole", "20 mg", "Viên nang", "Viên", new BigDecimal("1800"), 100, 1000, Medicine.InventoryType.MEDICINE),
                new MedicineSeed("TD050", "Metformin 500 mg", "Metformin", "500 mg", "Viên nén", "Viên", new BigDecimal("1000"), 100, 1000, Medicine.InventoryType.MEDICINE),
                new MedicineSeed("VT001", "Găng tay y tế không bột", "", "M", "Vật tư tiêu hao", "Đôi", new BigDecimal("1800"), 200, 2000, Medicine.InventoryType.SUPPLY),
                new MedicineSeed("VT002", "Bơm kim tiêm 5 mL", "", "5 mL", "Vật tư tiêu hao", "Cái", new BigDecimal("2500"), 100, 1000, Medicine.InventoryType.SUPPLY),
                new MedicineSeed("VT003", "Gạc vô khuẩn", "", "10 cm x 10 cm", "Vật tư tiêu hao", "Miếng", new BigDecimal("1200"), 200, 2000, Medicine.InventoryType.SUPPLY),
                new MedicineSeed("VT004", "Dây truyền dịch", "", "Tiêu chuẩn", "Vật tư tiêu hao", "Bộ", new BigDecimal("9000"), 50, 500, Medicine.InventoryType.SUPPLY),
                new MedicineSeed("VT005", "Khẩu trang y tế", "", "3 lớp", "Vật tư bảo hộ", "Cái", new BigDecimal("800"), 200, 3000, Medicine.InventoryType.SUPPLY)
        );
        LocalDateTime now = LocalDateTime.now();
        for (MedicineSeed seed : catalog) {
            Medicine medicine = medicines.findByCode(seed.code()).orElseGet(Medicine::new);
            if (medicine.getId() == null) {
                medicine.setCode(seed.code());
                medicine.setName(seed.name());
                medicine.setActiveIngredient(seed.ingredient());
                medicine.setStrength(seed.strength());
                medicine.setDosageForm(seed.form());
                medicine.setUnit(seed.unit());
                medicine.setPrice(seed.price());
                medicine.setMinStock(seed.minStock());
                medicine.setInventoryType(seed.inventoryType());
                medicine.setStatus(Medicine.Status.ACTIVE);
                medicine.setCreatedAt(now);
                medicine = medicines.save(medicine);
            } else if (medicine.getInventoryType() == null) {
                medicine.setInventoryType(seed.inventoryType());
                medicine = medicines.save(medicine);
            }
            if (batches.findByMedicine_IdAndBatchNumber(medicine.getId(), "KHO-MAC-DINH-2026").isEmpty()) {
                MedicineBatch batch = new MedicineBatch();
                batch.setMedicine(medicine);
                batch.setBatchNumber("KHO-MAC-DINH-2026");
                batch.setQuantity(seed.quantity());
                batch.setUnitPrice(seed.price());
                batch.setExpiryDate(LocalDate.now().plusYears(2));
                batch.setCreatedAt(now);
                batches.save(batch);
            }
        }
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
