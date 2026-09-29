package com.trungcang.trung_cang_his.controller;

import com.trungcang.trung_cang_his.service.AuthService;
import com.trungcang.trung_cang_his.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;
    private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();

    public AuthController(AuthService authService, UserRepository userRepository) {
        this.authService = authService;
        this.userRepository = userRepository;
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody LoginRequest request,
                                                     HttpServletRequest httpRequest,
                                                     HttpServletResponse httpResponse) {
        Map<String, Object> result = authService.login(request.username(), request.password());
        if (httpRequest.getSession(false) != null) httpRequest.changeSessionId();
        securityContextRepository.saveContext(SecurityContextHolder.getContext(), httpRequest, httpResponse);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> currentUser(Authentication authentication) {
        String fullName = userRepository.findByUsername(authentication.getName())
                .map(user -> user.getFullName())
                .orElse(authentication.getName());
        return ResponseEntity.ok(Map.of(
                "username", authentication.getName(),
                "fullName", fullName,
                "roles", authentication.getAuthorities().stream()
                        .map(authority -> authority.getAuthority())
                        .toList()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout() {
        return ResponseEntity.ok(Map.of("message", "Logout successful"));
    }

    public record LoginRequest(String username, String password) {
        @Override public String toString() { return "LoginRequest[password=REDACTED]"; }
    }
}
