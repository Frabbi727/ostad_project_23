package org.ostad.assignment_23.service;

import lombok.RequiredArgsConstructor;
import org.ostad.assignment_23.dto.AuthResponse;
import org.ostad.assignment_23.dto.LoginRequest;
import org.ostad.assignment_23.dto.RegisterRequest;
import org.ostad.assignment_23.entity.User;
import org.ostad.assignment_23.entity.VerificationToken;
import org.ostad.assignment_23.exception.*;
import org.ostad.assignment_23.repository.UserRepository;
import org.ostad.assignment_23.security.JwtUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final VerificationTokenService tokenService;
    private final EmailService emailService;
    private final JwtUtil jwtUtil;

    @Value("${app.email.rate-limit}")
    private long emailRateLimitMs;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException("User with this email already exists");
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setVerified(false);
        user.setLastVerificationEmailSent(LocalDateTime.now());

        user = userRepository.save(user);

        VerificationToken token = tokenService.createVerificationToken(user);
        emailService.sendVerificationEmail(user.getEmail(), token.getToken());

        return new AuthResponse("Registration successful. Please check your email to verify your account.");
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new InvalidCredentialsException("Invalid email or password");
        }

        if (!user.isVerified()) {
            checkAndSendVerificationEmail(user);
            throw new InvalidCredentialsException("Account not verified. A new verification email has been sent.");
        }

        String token = jwtUtil.generateToken(user.getEmail());
        return new AuthResponse(token, user.getEmail(), "Login successful");
    }

    @Transactional
    public AuthResponse verifyEmail(String tokenString) {
        VerificationToken token = tokenService.getVerificationToken(tokenString)
                .orElseThrow(() -> new InvalidTokenException("Invalid verification token"));

        if (!tokenService.isTokenValid(token)) {
            throw new InvalidTokenException("Verification token has expired or already been used");
        }

        User user = token.getUser();
        user.setVerified(true);
        userRepository.save(user);

        tokenService.markTokenAsUsed(token);

        return new AuthResponse("Email verified successfully. You can now login.");
    }

    @Transactional
    public void checkAndSendVerificationEmail(User user) {
        if (user.getLastVerificationEmailSent() != null) {
            LocalDateTime lastSent = user.getLastVerificationEmailSent();
            long minutesSinceLastEmail = Duration.between(lastSent, LocalDateTime.now()).toMillis();

            if (minutesSinceLastEmail < emailRateLimitMs) {
                long remainingSeconds = (emailRateLimitMs - minutesSinceLastEmail) / 1000;
                throw new EmailRateLimitException(
                        "Please wait " + remainingSeconds + " seconds before requesting another verification email"
                );
            }
        }

        VerificationToken token = tokenService.createVerificationToken(user);
        emailService.sendVerificationEmail(user.getEmail(), token.getToken());

        user.setLastVerificationEmailSent(LocalDateTime.now());
        userRepository.save(user);
    }
}
