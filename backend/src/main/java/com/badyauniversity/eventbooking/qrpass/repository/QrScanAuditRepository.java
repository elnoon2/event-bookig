package com.badyauniversity.eventbooking.qrpass.repository;

import com.badyauniversity.eventbooking.qrpass.model.QrScanAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QrScanAuditRepository extends JpaRepository<QrScanAudit, Long> {

    /** Most recent scan attempts, newest first (for the admin audit view). */
    List<QrScanAudit> findTop100ByOrderByIdDesc();

    List<QrScanAudit> findByJtiOrderByIdDesc(String jti);
}
