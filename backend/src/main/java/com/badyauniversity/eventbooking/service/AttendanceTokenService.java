package com.badyauniversity.eventbooking.service;

import com.badyauniversity.eventbooking.model.AttendanceToken;
import com.badyauniversity.eventbooking.model.User;
import com.badyauniversity.eventbooking.repository.AttendanceTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.UUID;

@Service
@Transactional
public class AttendanceTokenService {

    private final AttendanceTokenRepository attendanceTokenRepository;

    @Value("${app.attendance.tokenPepper:change-me}")
    private String tokenPepper;

    @Value("${app.attendance.tokenTtlMinutes:10}")
    private long tokenTtlMinutes;

    public AttendanceTokenService(AttendanceTokenRepository attendanceTokenRepository) {
        this.attendanceTokenRepository = attendanceTokenRepository;
    }

    public record IssuedToken(String rawToken, LocalDateTime expiresAt) {
    }

    public IssuedToken issueForUser(User user) {
        // Instead of deleting, mark previous active tokens as EXPIRED
        java.util.List<AttendanceToken> activeTokens = attendanceTokenRepository.findByUser_IdAndStatus(user.getId(), "ACTIVE");
        for (AttendanceToken t : activeTokens) {
            t.setStatus("EXPIRED");
            attendanceTokenRepository.save(t);
        }

        String raw = UUID.randomUUID().toString();
        String hash = sha256Hex(raw + ":" + tokenPepper);

        AttendanceToken row = new AttendanceToken();
        row.setUser(user);
        row.setTokenHash(hash);
        row.setExpiresAt(LocalDateTime.now().plusMinutes(tokenTtlMinutes));
        row.setStatus("ACTIVE");
        attendanceTokenRepository.save(row);

        return new IssuedToken(raw, row.getExpiresAt());
    }

    public AttendanceToken consume(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new RuntimeException("Missing token");
        }
        String hash = sha256Hex(rawToken.trim() + ":" + tokenPepper);
        AttendanceToken row = attendanceTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new RuntimeException("Invalid or expired token"));

        if (row.getUsedAt() != null || "USED".equals(row.getStatus())) {
            throw new RuntimeException("Token already used");
        }
        if (row.getExpiresAt().isBefore(LocalDateTime.now()) || "EXPIRED".equals(row.getStatus())) {
            if ("ACTIVE".equals(row.getStatus())) {
                row.setStatus("EXPIRED");
                attendanceTokenRepository.save(row);
            }
            throw new RuntimeException("Token expired");
        }

        row.setUsedAt(LocalDateTime.now());
        row.setStatus("USED");
        return attendanceTokenRepository.save(row);
    }

    public String getTokenStatus(Long userId) {
        java.util.Optional<AttendanceToken> latestOpt = attendanceTokenRepository.findFirstByUser_IdOrderByIdDesc(userId);
        if (latestOpt.isEmpty()) {
            return "EXPIRED";
        }
        AttendanceToken latest = latestOpt.get();
        if ("ACTIVE".equals(latest.getStatus())) {
            if (latest.getExpiresAt().isBefore(LocalDateTime.now())) {
                latest.setStatus("EXPIRED");
                attendanceTokenRepository.save(latest);
                return "EXPIRED";
            }
        }
        return latest.getStatus();
    }

    public void cleanupExpired() {
        attendanceTokenRepository.deleteByExpiresAtBefore(LocalDateTime.now().minusMinutes(1));
    }

    private static String sha256Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            throw new RuntimeException("Token hashing failed", e);
        }
    }
}

