package org.ostad.assignment_23.service;

import lombok.RequiredArgsConstructor;
import org.ostad.assignment_23.entity.User;
import org.ostad.assignment_23.entity.VerificationToken;
import org.ostad.assignment_23.repository.VerificationTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VerificationTokenService {

    private final VerificationTokenRepository tokenRepository;

    @Value("${app.verification.token.expiration}")
    private long tokenExpirationMs;

    @Transactional
    public VerificationToken createVerificationToken(User user) {
        tokenRepository.deleteByUser(user);

        VerificationToken token = new VerificationToken();
        token.setToken(UUID.randomUUID().toString());
        token.setUser(user);
        token.setExpiryDate(LocalDateTime.now().plusSeconds(tokenExpirationMs / 1000));
        token.setUsed(false);

        return tokenRepository.save(token);
    }

    public Optional<VerificationToken> getVerificationToken(String token) {
        return tokenRepository.findByToken(token);
    }

    public boolean isTokenValid(VerificationToken token) {
        return !token.isUsed() && token.getExpiryDate().isAfter(LocalDateTime.now());
    }

    @Transactional
    public void markTokenAsUsed(VerificationToken token) {
        token.setUsed(true);
        tokenRepository.save(token);
    }
}
