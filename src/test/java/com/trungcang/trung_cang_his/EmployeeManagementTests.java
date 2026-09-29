package com.trungcang.trung_cang_his;

import com.trungcang.trung_cang_his.domain.User;
import com.trungcang.trung_cang_his.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class EmployeeManagementTests {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder passwords;
    @Autowired @Qualifier("seedUsers") CommandLineRunner seeder;

    private String profile(String role, String status) {
        return """
                {"fullName":"Nhân viên thử nghiệm","employeeCode":"NV-TEST","phone":"0901234567",
                 "email":"staff@example.com","dateOfBirth":"1990-01-02","gender":"FEMALE",
                 "address":"Địa chỉ thử nghiệm","identityNumber":"012345678901",
                 "department":"Khoa khám bệnh","jobTitle":"Nhân viên","specialization":"Nội khoa",
                 "roleCode":"%s","status":"%s"}
                """.formatted(role, status);
    }

    private Long createStaff() throws Exception {
        mvc.perform(post("/api/employees").with(user("admin").roles("ADMIN")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("""
                        {"username":"test.staff","password":"Original123!","profile":%s}
                        """.formatted(profile("DOCTOR", "ACTIVE"))))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.username").value("test.staff"))
                .andExpect(jsonPath("$.password").doesNotExist());
        return users.findByUsername("test.staff").orElseThrow().getId();
    }

    private MockHttpSession login(String username, String password) throws Exception {
        var result = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk()).andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    @ParameterizedTest
    @ValueSource(strings = {"DOCTOR", "RECEPTION", "PHARMACIST"})
    void onlyAdminCanReadOrModifyEmployees(String role) throws Exception {
        mvc.perform(get("/employees").with(user("tester").roles(role))).andExpect(status().isForbidden());
        for (String path : new String[]{"/api/employees", "/api/employees/roles"})
            mvc.perform(get(path).with(user("tester").roles(role))).andExpect(status().isForbidden());
        mvc.perform(post("/api/employees").with(user("tester").roles(role)).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isForbidden());
        mvc.perform(put("/api/employees/1").with(user("tester").roles(role)).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isForbidden());
        mvc.perform(put("/api/employees/1/password").with(user("tester").roles(role)).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"password\":\"Changed123!\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/employees/1").with(user("tester").roles(role)).with(csrf()))
                .andExpect(status().isForbidden());
        mvc.perform(get("/rooms").with(user("tester").roles(role)))
                .andExpect(content().string(not(containsString("href=\"/employees\""))));
    }

    @Test
    void adminPageRendersAndWritesRequireCsrf() throws Exception {
        mvc.perform(get("/employees").with(user("admin").roles("ADMIN"))).andExpect(status().isOk())
                .andExpect(content().string(containsString("staffForm")))
                .andExpect(content().string(containsString("href=\"/employees\"")))
                .andExpect(content().string(not(containsString("th:value"))));
        mvc.perform(get("/api/employees")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/employees").with(user("admin").roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isForbidden());
    }

    @Test
    void profileAndPermissionsCanBeEditedWithoutExposingPassword() throws Exception {
        Long id = createStaff();
        String encoded = users.findById(id).orElseThrow().getPassword();
        assertThat(passwords.matches("Original123!", encoded)).isTrue();
        mvc.perform(put("/api/employees/" + id).with(user("admin").roles("ADMIN")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(profile("PHARMACIST", "ACTIVE")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.roleCode").value("PHARMACIST"))
                .andExpect(jsonPath("$.dateOfBirth").value("1990-01-02"))
                .andExpect(jsonPath("$.address").value("Địa chỉ thử nghiệm"));
        assertThat(users.findById(id).orElseThrow().getPassword()).isEqualTo(encoded);
        mvc.perform(get("/api/employees").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk()).andExpect(content().string(not(containsString("password"))))
                .andExpect(content().string(not(containsString(encoded))));
    }

    @Test
    void changingRoleRevokesExistingSessionAndNewLoginUsesNewRole() throws Exception {
        Long id = createStaff();
        MockHttpSession session = login("test.staff", "Original123!");
        mvc.perform(get("/examination").session(session)).andExpect(status().isOk());
        mvc.perform(put("/api/employees/" + id).with(user("admin").roles("ADMIN")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(profile("RECEPTION", "ACTIVE")))
                .andExpect(status().isOk());
        mvc.perform(get("/api/auth/me").session(session)).andExpect(status().isUnauthorized());
        MockHttpSession fresh = login("test.staff", "Original123!");
        mvc.perform(get("/examination").session(fresh)).andExpect(status().isForbidden());
        mvc.perform(get("/reception").session(fresh)).andExpect(status().isOk());
    }

    @Test
    void passwordResetRevokesSessionAndRejectsOldPassword() throws Exception {
        Long id = createStaff();
        MockHttpSession session = login("test.staff", "Original123!");
        mvc.perform(put("/api/employees/" + id + "/password").with(user("admin").roles("ADMIN")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"password\":\"Changed123!\"}"))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/auth/me").session(session)).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"test.staff\",\"password\":\"Original123!\"}"))
                .andExpect(status().isUnauthorized());
        assertThat(login("test.staff", "Changed123!")).isNotNull();
    }

    @Test
    void disabledAndDeletedAccountsCannotLogInAndDeletedRecordsRetainIdentity() throws Exception {
        Long id = createStaff();
        MockHttpSession session = login("test.staff", "Original123!");
        mvc.perform(put("/api/employees/" + id).with(user("admin").roles("ADMIN")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(profile("DOCTOR", "INACTIVE")))
                .andExpect(status().isOk());
        mvc.perform(get("/api/auth/me").session(session)).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"test.staff\",\"password\":\"Original123!\"}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(delete("/api/employees/" + id).with(user("admin").roles("ADMIN")).with(csrf()))
                .andExpect(status().isNoContent());
        assertThat(users.findById(id).orElseThrow().isDeleted()).isTrue();
        mvc.perform(get("/api/employees").with(user("admin").roles("ADMIN")))
                .andExpect(content().string(not(containsString("test.staff"))));
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"test.staff\",\"password\":\"Original123!\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsDuplicatesInvalidRolesShortPasswordsAndSelfLockout() throws Exception {
        Long id = createStaff();
        mvc.perform(post("/api/employees").with(user("admin").roles("ADMIN")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("""
                {"username":"TEST.STAFF","password":"Original123!","profile":%s}
                """.formatted(profile("DOCTOR", "ACTIVE")))).andExpect(status().isConflict());
        mvc.perform(post("/api/employees").with(user("admin").roles("ADMIN")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("""
                {"username":"another.staff","password":"Original123!","profile":%s}
                """.formatted(profile("DOCTOR", "ACTIVE")))).andExpect(status().isConflict());
        mvc.perform(put("/api/employees/" + id).with(user("admin").roles("ADMIN")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(profile("UNKNOWN", "ACTIVE")))
                .andExpect(status().isBadRequest());
        mvc.perform(put("/api/employees/" + id + "/password").with(user("admin").roles("ADMIN")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"password\":\"short\"}"))
                .andExpect(status().isBadRequest());
        Long adminId = users.findByUsername("admin").orElseThrow().getId();
        mvc.perform(delete("/api/employees/" + adminId).with(user("admin").roles("ADMIN")).with(csrf()))
                .andExpect(status().isConflict());
        mvc.perform(put("/api/employees/" + adminId).with(user("admin").roles("ADMIN")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(profile("DOCTOR", "ACTIVE")))
                .andExpect(status().isConflict());
        mvc.perform(put("/api/employees/" + adminId).with(user("admin").roles("ADMIN")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(profile("ADMIN", "INACTIVE")))
                .andExpect(status().isConflict());
    }

    @Test
    void seedingDoesNotOverwriteAdministrativeChangesOrReviveDeletedStaff() throws Exception {
        User doctor = users.findByUsername("bacsi").orElseThrow();
        doctor.setFullName("Tên đã chỉnh sửa");
        doctor.setPassword(passwords.encode("Preserved123!"));
        doctor.setRole(users.findByUsername("letan").orElseThrow().getRole());
        doctor.setStatus(User.Status.INACTIVE);
        doctor.setDeleted(true);
        users.saveAndFlush(doctor);
        seeder.run();
        User after = users.findByUsername("bacsi").orElseThrow();
        assertThat(after.getFullName()).isEqualTo("Tên đã chỉnh sửa");
        assertThat(passwords.matches("Preserved123!", after.getPassword())).isTrue();
        assertThat(after.getRole().getCode()).isEqualTo("RECEPTION");
        assertThat(after.getStatus()).isEqualTo(User.Status.INACTIVE);
        assertThat(after.isDeleted()).isTrue();
    }
}
