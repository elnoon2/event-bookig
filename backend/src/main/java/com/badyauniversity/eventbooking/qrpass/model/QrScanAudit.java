package com.badyauniversity.eventbooking.qrpass.model;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * Immutable record of a single scan/validation attempt. Every call to validate — successful or not —
 * writes one row here, giving a tamper-evident audit trail for security review and abuse detection.
 */
@Entity
@Table(name = "qr_scan_audit", indexes = {
        @Index(name = "idx_qr_scan_audit_jti", columnList = "jti"),
        @Index(name = "idx_qr_scan_audit_created", columnList = "created_at")
})
public class QrScanAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    /** The token id from the scanned payload, if one could be extracted (else null). */
    @Column(name = "jti", length = 64)
    private String jti;

    @Enumerated(EnumType.STRING)
    @Column(name = "result", nullable = false, length = 20)
    private ValidationStatus result;

    @Column(name = "ip", length = 64)
    private String ip;

    @Column(name = "user_agent", length = 255)
    private String userAgent;

    /** Who performed the scan (e.g. the gate operator), if provided. */
    @Column(name = "scanned_by", length = 150)
    private String scannedBy;

    public QrScanAudit() {
    }

    public QrScanAudit(String jti, ValidationStatus result, String ip, String userAgent, String scannedBy) {
        this.createdAt = Instant.now();
        this.jti = jti;
        this.result = result;
        this.ip = ip;
        this.userAgent = truncate(userAgent, 255);
        this.scannedBy = truncate(scannedBy, 150);
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return null;
        }
        return s.length() <= max ? s : s.substring(0, max);
    }

    public Long getId() {
        return id;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public String getJti() {
        return jti;
    }

    public void setJti(String jti) {
        this.jti = jti;
    }

    public ValidationStatus getResult() {
        return result;
    }

    public void setResult(ValidationStatus result) {
        this.result = result;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public String getScannedBy() {
        return scannedBy;
    }

    public void setScannedBy(String scannedBy) {
        this.scannedBy = scannedBy;
    }
}
