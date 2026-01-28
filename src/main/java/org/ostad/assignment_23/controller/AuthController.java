package org.ostad.assignment_23.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.ostad.assignment_23.dto.AuthResponse;
import org.ostad.assignment_23.dto.LoginRequest;
import org.ostad.assignment_23.dto.RegisterRequest;
import org.ostad.assignment_23.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/verify")
    public ResponseEntity<AuthResponse> verifyEmail(@RequestParam String token) {
        AuthResponse response = authService.verifyEmail(token);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/test")
    public ResponseEntity<String> testProtectedEndpoint() {
        return ResponseEntity.ok("This is a protected endpoint. You are authenticated!");
    }
}
