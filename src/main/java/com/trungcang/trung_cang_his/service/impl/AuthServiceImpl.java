package com.trungcang.trung_cang_his.service.impl;

import com.trungcang.trung_cang_his.service.AuthService;
import com.trungcang.trung_cang_his.repository.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;

    public AuthServiceImpl(AuthenticationManager authenticationManager, UserRepository userRepository) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
    }

    @Override
    public Map<String, Object> login(String username, String password) {
        Authentication authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(username, password));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        Map<String, Object> result = new HashMap<>();
        result.put("status", "success");
        result.put("message", "Login successful");
        result.put("username", authentication.getName());
        result.put("fullName", userRepository.findByUsername(authentication.getName())
                .map(user -> user.getFullName())
                .orElse(authentication.getName()));
        result.put("roles", authentication.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .toList());
        return result;
    }
}
