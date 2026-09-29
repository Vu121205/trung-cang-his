package com.trungcang.trung_cang_his.config;

import com.trungcang.trung_cang_his.domain.User;
import com.trungcang.trung_cang_his.repository.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.http.HttpMethod;
import jakarta.servlet.http.HttpServletResponse;

import jakarta.servlet.DispatcherType;
import java.util.List;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, UserRepository users) throws Exception {
        http
                .csrf(csrf -> csrf.requireCsrfProtectionMatcher(request ->
                        request.getRequestURI().startsWith(request.getContextPath() + "/api/employees")
                                && !List.of("GET", "HEAD", "OPTIONS", "TRACE").contains(request.getMethod())))
                .authorizeHttpRequests(auth -> auth
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers("/WEB-INF/**").denyAll()
                        .requestMatchers("/", "/login", "/resources/**", "/api/auth/login").permitAll()
                        .requestMatchers("/api/auth/me").authenticated()
                        .requestMatchers("/employees", "/api/employees", "/api/employees/**").hasRole("ADMIN")
                        .requestMatchers("/api/workflow/**").hasAnyRole("ADMIN", "PHARMACIST")
                        .requestMatchers("/rooms", "/examination-history").hasAnyRole("ADMIN", "DOCTOR", "RECEPTION", "PHARMACIST")
                        .requestMatchers(HttpMethod.GET, "/api/examination-history", "/api/examination-history/**")
                        .hasAnyRole("ADMIN", "DOCTOR", "RECEPTION", "PHARMACIST")
                        .requestMatchers("/reception").hasAnyRole("ADMIN", "RECEPTION")
                        .requestMatchers("/examination", "/medical-examination").hasAnyRole("ADMIN", "DOCTOR")
                        .requestMatchers("/pharmacy", "/payment").hasAnyRole("ADMIN", "PHARMACIST")
                        .requestMatchers(HttpMethod.GET, "/api/patients/**", "/api/visits/**",
                                "/api/departments/**", "/api/examination-rooms/**")
                        .hasAnyRole("ADMIN", "DOCTOR", "RECEPTION", "PHARMACIST")
                        .requestMatchers("/api/patients/**").hasAnyRole("ADMIN", "RECEPTION")
                        .requestMatchers("/api/visits/**").hasAnyRole("ADMIN", "RECEPTION", "DOCTOR")
                        .requestMatchers(HttpMethod.GET, "/api/medicines/**", "/api/prescriptions/**")
                        .hasAnyRole("ADMIN", "DOCTOR", "PHARMACIST")
                        .requestMatchers("/api/examinations/**", "/api/service-orders/**",
                                "/api/service-results/**", "/api/prescriptions/**")
                        .hasAnyRole("ADMIN", "DOCTOR")
                        .requestMatchers("/api/medicines/**", "/api/invoices/**").hasAnyRole("ADMIN", "PHARMACIST")
                        .requestMatchers(HttpMethod.GET, "/api/diagnoses/**", "/api/medical-services/**")
                        .hasAnyRole("ADMIN", "DOCTOR")
                        .anyRequest().hasRole("ADMIN"))
                .exceptionHandling(errors -> errors.authenticationEntryPoint((request, response, exception) -> {
                    if (request.getRequestURI().startsWith(request.getContextPath() + "/api/")) {
                        response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                    } else {
                        response.sendRedirect("/login");
                    }
                }))
                .addFilterBefore(new StaffSessionFilter(users), org.springframework.security.web.access.intercept.AuthorizationFilter.class)
                .formLogin(form -> form.disable())
                .logout(logout -> logout.logoutUrl("/api/auth/logout").permitAll());
        return http.build();
    }

    @Bean
    UserDetailsService userDetailsService(UserRepository userRepository) {
        return username -> {
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

            return new StaffPrincipal(user);
        };
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }
}
