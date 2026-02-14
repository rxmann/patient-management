package com.pm.auth_service.controller;

import com.pm.auth_service.dto.LoginRequestDTO;
import com.pm.auth_service.dto.LoginResponseDTO;
import com.pm.auth_service.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@Validated
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "Generate token on user login")
    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@Valid @RequestBody LoginRequestDTO loginRequestDto) {
        log.info("Login request received: {}", loginRequestDto.toString());
        Optional<String> optToken = authService.authenticate(loginRequestDto);
        return optToken.map(s -> ResponseEntity.ok(new LoginResponseDTO(s))).orElseGet(() -> ResponseEntity.badRequest().build());
    }

    @Operation(summary = "validate token.")
    @GetMapping("/validate")
    public ResponseEntity<Void> validateToken(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        return Optional.ofNullable(authHeader)
                .filter(header -> header.startsWith("Bearer "))
                .map(header -> header.substring(7))
                .flatMap(authService::validateToken)
                .filter(isValid -> isValid) // Boolean::booleanValue
                .map(success -> ResponseEntity.ok().<Void>build())
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }

}