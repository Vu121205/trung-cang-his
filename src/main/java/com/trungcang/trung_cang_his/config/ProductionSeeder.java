package com.trungcang.trung_cang_his.config;

import com.trungcang.trung_cang_his.domain.Role;
import com.trungcang.trung_cang_his.domain.User;
import com.trungcang.trung_cang_his.repository.RoleRepository;
import com.trungcang.trung_cang_his.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;

@Configuration
@Profile("prod")
public class ProductionSeeder {
    @Bean
    public CommandLineRunner seedProductionAdmin(RoleRepository roles, UserRepository users,
            PasswordEncoder encoder, PlatformTransactionManager transactions,
            @Value("${BOOTSTRAP_ADMIN_USERNAME:admin}") String username,
            @Value("${BOOTSTRAP_ADMIN_PASSWORD:}") String password) {
        return args -> new TransactionTemplate(transactions).executeWithoutResult(transaction -> {
            LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
            Map.of("ADMIN", "Admin", "DOCTOR", "Bác Sĩ", "RECEPTION", "Lễ Tân", "PHARMACIST", "Dược Sĩ")
                    .forEach((code, name) -> {
                        if (roles.findByCode(code).isPresent()) return;
                        Role role = new Role();
                        role.setCode(code);
                        role.setName(name);
                        role.setCreatedAt(now);
                        roles.save(role);
                    });
            // Restarting must never reset an account managed through the application.
            if (users.existsByUsername(username)) return;
            if (username.isBlank() || username.length() > 100 || password.length() < 12
                    || password.getBytes(StandardCharsets.UTF_8).length > 72) {
                throw new IllegalStateException("Set BOOTSTRAP_ADMIN_USERNAME and BOOTSTRAP_ADMIN_PASSWORD "
                        + "(12 or more characters, at most 72 UTF-8 bytes) before first production startup.");
            }
            User admin = new User();
            admin.setUsername(username);
            admin.setPassword(encoder.encode(password));
            admin.setFullName("Quản trị viên");
            admin.setRole(roles.findByCode("ADMIN").orElseThrow());
            admin.setStatus(User.Status.ACTIVE);
            admin.setCreatedAt(now);
            admin.setUpdatedAt(now);
            users.save(admin);
        });
    }
}
