package com.trungcang.trung_cang_his;

import com.trungcang.trung_cang_his.config.ProductionSeeder;
import com.trungcang.trung_cang_his.repository.PatientRepository;
import com.trungcang.trung_cang_his.repository.RoleRepository;
import com.trungcang.trung_cang_his.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;

import static org.junit.jupiter.api.Assertions.*;

@ActiveProfiles("prod")
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:production-startup;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "BOOTSTRAP_ADMIN_PASSWORD=test-only-admin-password"
})
class ProductionStartupTests {
    @Autowired UserRepository users;
    @Autowired RoleRepository roles;
    @Autowired PatientRepository patients;
    private final ProductionSeeder seeder = new ProductionSeeder();
    @Autowired PasswordEncoder encoder;
    @Autowired PlatformTransactionManager transactions;

    @Test
    void productionCreatesOnlyAdminAndRolesWithoutDemoPatients() {
        assertEquals(1, users.count());
        assertEquals(4, roles.count());
        assertEquals(0, patients.count());
        var admin = users.findByUsername("admin").orElseThrow();
        assertTrue(encoder.matches("test-only-admin-password", admin.getPassword()));
        assertFalse(users.existsByUsername("bacsi"));
    }

    @Test
    void restartingDoesNotResetAdminPassword() throws Exception {
        var original = users.findByUsername("admin").orElseThrow().getPassword();
        seeder.seedProductionAdmin(roles, users, encoder, transactions, "admin", "another-test-password").run();
        assertEquals(original, users.findByUsername("admin").orElseThrow().getPassword());
    }

    @Test
    void firstStartupRejectsMissingBootstrapPassword() {
        assertThrows(IllegalStateException.class,
                () -> seeder.seedProductionAdmin(roles, users, encoder, transactions, "new-admin", "").run());
        assertFalse(users.existsByUsername("new-admin"));
    }
}
