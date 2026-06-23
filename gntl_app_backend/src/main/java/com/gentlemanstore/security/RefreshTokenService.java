package com.gentlemanstore.security;

import com.gentlemanstore.common.exception.InvalidRefreshTokenException;
import com.gentlemanstore.security.model.RefreshToken;
import com.gentlemanstore.security.repository.RefreshTokenRepository;
import com.gentlemanstore.user.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository repo;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${jwt.refresh-expiration}")
    private long refreshExpirationMs;

    @Transactional
    public String issueRefreshToken(User user) {
        byte[] randomBytes = new byte[64];
        secureRandom.nextBytes(randomBytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .tokenHash(hash(rawToken))
                .expiresAt(LocalDateTime.now().plusNanos(refreshExpirationMs * 1_000_000))
                .revoked(false)
                .build();

        repo.save(refreshToken);
        return rawToken;
    }

    @Transactional
    public RefreshToken consumeRefreshToken(String rawToken) {
        RefreshToken refreshToken = repo.findByTokenHash(hash(rawToken))
                .orElseThrow(() -> new InvalidRefreshTokenException("Invalid refresh token"));

        if (refreshToken.isRevoked() || refreshToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new InvalidRefreshTokenException("Refresh token expired or revoked");
        }

        refreshToken.setRevoked(true);
        repo.save(refreshToken);

        return refreshToken;
    }

    @Transactional
    public void revokeRefreshToken(String rawToken) {
        repo.findByTokenHash(hash(rawToken)).ifPresent(refreshToken -> {
            refreshToken.setRevoked(true);
            repo.save(refreshToken);
        });
    }

    @Transactional
    public void revokeAllForUser(Long userId) {
        repo.revokeAllByUserId(userId);
    }

    private String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(rawToken.getBytes());
            return Base64.getEncoder().encodeToString(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
