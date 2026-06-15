package com.badyauniversity.eventbooking.qrpass.repository;

import com.badyauniversity.eventbooking.qrpass.model.QrPass;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface QrPassRepository extends JpaRepository<QrPass, Long> {

    Optional<QrPass> findByJti(String jti);

    /**
     * Pessimistically locks the pass row for the duration of the validation transaction so two
     * concurrent scans of a one-time pass cannot both consume it (the second blocks, then sees the
     * already-incremented use count and is rejected as ALREADY_USED).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from QrPass p where p.jti = :jti")
    Optional<QrPass> findByJtiForUpdate(@Param("jti") String jti);
}
