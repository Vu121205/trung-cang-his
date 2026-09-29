package com.trungcang.trung_cang_his;

import com.trungcang.trung_cang_his.domain.Role;
import com.trungcang.trung_cang_his.domain.User;
import com.trungcang.trung_cang_his.repository.PatientRepository;
import com.trungcang.trung_cang_his.repository.RoleRepository;
import com.trungcang.trung_cang_his.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AccessControlTests {
    @Autowired MockMvc mvc;
    @Autowired RoleRepository roles;
    @Autowired UserRepository users;
    @Autowired PatientRepository patients;
    @Autowired PlatformTransactionManager transactions;
    @Autowired @Qualifier("seedUsers") CommandLineRunner seeder;

    @ParameterizedTest
    @CsvSource({"admin,ADMIN,Admin", "bacsi,DOCTOR,bác sĩ", "duocsi,PHARMACIST,dược sĩ", "letan,RECEPTION,lễ tân"})
    void seededUsersCanLogInAndUseSession(String username, String role, String fullName) throws Exception {
        var login = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"pkdktc68@\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.fullName").value(fullName))
                .andExpect(jsonPath("$.roles[0]").value("ROLE_" + role)).andReturn();
        var session = (MockHttpSession) login.getRequest().getSession(false);
        assertThat(session).isNotNull();
        mvc.perform(get("/api/auth/me").session(session)).andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(username));
        mvc.perform(get("/rooms").session(session)).andExpect(status().isOk());
    }

    @ParameterizedTest
    @CsvSource({"ADMIN,true,true,true", "DOCTOR,false,true,false", "RECEPTION,true,false,false", "PHARMACIST,false,false,true"})
    void menusRoomOptionsAndRoutesMatchRole(String role, boolean reception, boolean examination, boolean pharmacy) throws Exception {
        var allowed = Map.of("/rooms", true, "/examination-history", true, "/reception", reception, "/examination", examination,
                "/medical-examination", examination, "/pharmacy", pharmacy, "/payment", pharmacy);
        for (var route : allowed.entrySet()) {
            var result = mvc.perform(get(route.getKey()).with(user("tester").roles(role)))
                    .andExpect(status().is(route.getValue() ? 200 : 403)).andReturn();
            if (route.getValue()) {
                String html = result.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
                assertThat(html).contains("navbar-nav", "<footer", "/resources/js/main.js");
                assertThat(html).doesNotContain("th:replace", "th:if");
                String menu = html.substring(html.indexOf("<ul"), html.indexOf("</ul>"));
                for (var link : allowed.entrySet()) {
                    if (link.getKey().equals("/medical-examination")) continue;
                    assertThat(menu.contains("href=\"" + link.getKey() + "\""))
                            .as("%s menu on %s for %s", link.getKey(), route.getKey(), role).isEqualTo(link.getValue());
                }
                if (route.getKey().equals("/rooms")) {
                    assertThat(html.contains("<option value=\"Khoa khám bệnh\"" )).isEqualTo(examination);
                    assertThat(html.contains("<option value=\"Khoa tiếp nhận\"" )).isEqualTo(reception);
                    assertThat(html.contains("<option value=\"Kế toán\"" )).isEqualTo(pharmacy);
                }
            }
        }
    }

    @Test
    void anonymousRequestsAndRawTemplatesAreProtected() throws Exception {
        mvc.perform(get("/login")).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("loginForm")));
        mvc.perform(get("/resources/js/main.js")).andExpect(status().isOk());
        mvc.perform(get("/rooms")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login"));
        mvc.perform(get("/api/patients")).andExpect(status().isUnauthorized());
        for (String path : List.of("/WEB-INF/client/reception.html", "/WEB-INF/fragments/sidebar.html")) {
            mvc.perform(get(path).with(user("admin").roles("ADMIN"))).andExpect(status().isForbidden());
        }
    }

    @ParameterizedTest
    @CsvSource({"ADMIN", "DOCTOR", "RECEPTION", "PHARMACIST"})
    void apiPermissionsMatchResponsibilities(String role) throws Exception {
        mvc.perform(get("/api/patients").with(user("tester").roles(role))).andExpect(status().isOk());
        var reads = Map.of("/api/examinations", role.equals("ADMIN") || role.equals("DOCTOR"),
                "/api/invoices", role.equals("ADMIN") || role.equals("PHARMACIST"),
            "/api/prescriptions", !role.equals("RECEPTION"),
                "/api/medicines", !role.equals("RECEPTION"));
        for (var entry : reads.entrySet()) {
            var result = mvc.perform(get(entry.getKey()).with(user("tester").roles(role)))
                .andExpect(status().is(entry.getValue() ? 200 : 403)).andReturn();
            if (entry.getKey().equals("/api/prescriptions") && entry.getValue()) {
            assertThat(result.getResponse().getContentAsString()).doesNotContain("password", "$2a$");
            }
        }
        if (role.equals("DOCTOR") || role.equals("PHARMACIST")) {
            for (String method : List.of("POST", "PUT", "DELETE")) {
                String path = method.equals("POST") ? "/api/patients" : "/api/patients/1";
                mvc.perform(request(org.springframework.http.HttpMethod.valueOf(method), path)
                                .with(user("tester").roles(role)).contentType(MediaType.APPLICATION_JSON).content("{}"))
                        .andExpect(status().isForbidden());
            }
        }
        if (!role.equals("ADMIN")) {
            mvc.perform(post("/api/departments").with(user("tester").roles(role))
                    .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isForbidden());
        }
        if (role.equals("RECEPTION")) {
            mvc.perform(post("/api/patients").with(user("tester").roles(role)).contentType(MediaType.APPLICATION_JSON)
                    .content("{\"patientCode\":\"TEST-RECEPTION\",\"fullName\":\"Test patient\",\"phone\":\"0901234567\"}"))
                    .andExpect(status().isOk());
        }
    }

    @Test
    void migrationPreservesExistingUsersAndPatientsAndIsRepeatable() throws Exception {
        long patientCount = patients.count();
        new TransactionTemplate(transactions).executeWithoutResult(transaction -> {
            Role unusedRole = new Role();
            unusedRole.setCode("TECHNICIAN");
            unusedRole.setName("Kỹ thuật viên");
            roles.save(unusedRole);
            for (String code : List.of("NURSE", "ACCOUNTANT", "ROLE_DOCTOR", "RECEPTIONIST", "CASHIER")) {
                Role role = new Role();
                role.setCode(code);
                role.setName(code);
                roles.save(role);
                User staff = new User();
                staff.setUsername("legacy-" + code);
                staff.setFullName("Existing staff");
                staff.setPassword("unchanged");
                staff.setRole(role);
                users.save(staff);
            }
        });
        long userCount = users.count();
        seeder.run();
        seeder.run();
        assertThat(users.count()).isEqualTo(userCount);
        assertThat(patients.count()).isEqualTo(patientCount);
        assertThat(roles.findAll()).extracting(Role::getCode)
                .containsExactlyInAnyOrder("ADMIN", "DOCTOR", "RECEPTION", "PHARMACIST");
        new TransactionTemplate(transactions).executeWithoutResult(transaction -> {
            for (var mapping : Map.of("NURSE", "RECEPTION", "ACCOUNTANT", "PHARMACIST", "ROLE_DOCTOR", "DOCTOR",
                    "RECEPTIONIST", "RECEPTION", "CASHIER", "PHARMACIST").entrySet()) {
                User staff = users.findByUsername("legacy-" + mapping.getKey()).orElseThrow();
                assertThat(staff.getRole().getCode()).isEqualTo(mapping.getValue());
                assertThat(staff.getPassword()).isEqualTo("unchanged");
                assertThat(staff.getFullName()).isEqualTo("Existing staff");
            }
        });
    }
}
