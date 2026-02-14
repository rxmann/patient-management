package com.pm.auth_service.service;

import com.pm.auth_service.dto.LoginRequestDTO;
import com.pm.auth_service.util.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthService(UserService userService, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    public Optional<String> authenticate(LoginRequestDTO loginRequestDTO) {
        log.info("Authenticating user: {}, email: {}", loginRequestDTO.toString(), loginRequestDTO.getEmail());
        return userService
                .findByEmail(loginRequestDTO.getEmail())
                .filter(u -> passwordEncoder.matches(loginRequestDTO.getPassword(), u.getPassword()))
                .map(u -> jwtUtil.generateToken(u.getEmail(), u.getRole()));
    }

    public Optional<Boolean> validateToken(String token) {
        try {
            jwtUtil.validateToken(token);
            return Optional.of(Boolean.TRUE);
        } catch (Exception e) {
            log.error(e.getMessage());
            return Optional.empty();
        }
    }
}