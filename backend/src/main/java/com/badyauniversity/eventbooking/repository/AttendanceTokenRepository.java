package com.badyauniversity.eventbooking.repository;

import com.badyauniversity.eventbooking.model.AttendanceToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface AttendanceTokenRepository extends JpaRepository<AttendanceToken, Long> {

    Optional<AttendanceToken> findByTokenHash(String tokenHash);

    long deleteByUser_Id(Long userId);

    long deleteByExpiresAtBefore(LocalDateTime cutoff);

    java.util.List<AttendanceToken> findByUser_IdAndStatus(Long userId, String status);

    java.util.Optional<AttendanceToken> findFirstByUser_IdOrderByIdDesc(Long userId);
}

