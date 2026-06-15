package com.badyauniversity.eventbooking.qrpass.model;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * A signed, server-side QR pass. The QR code itself only carries the {@code jti} plus an HMAC
 * signature — every authoritative attribute (status, expiry, remaining uses, holder identity) lives
 * here in the database and is never trusted from the scanned payload.
 *
 * Lifecycle status: ACTIVE -> USED (consumed) or ACTIVE -> REVOKED (by an admin). Expiry is derived
 * from {@link #expiresAt} rather than stored as a status, so it is always accurate.
 */
@Entity
@Table(name = "qr_passes", indexes = {
        @Index(name = "idx_qr_passes_status", columnList = "status"),
        @Index(name = "idx_qr_passes_expires", columnList = "expires_at")
})
public class QrPass {

    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_USED = "USED";
    public static final String STATUS_REVOKED = "REVOKED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Unique token id (UUID). This is the only pass reference embedded in the QR payload. */
    @Column(name = "jti", nullable = false, unique = true, length = 64)
    private String jti;

    /** What the pass grants access to, e.g. TICKET / ACCESS / USER (free-form, caller defined). */
    @Column(name = "subject_type", length = 40)
    private String subjectType;

    /** Optional reference id of the subject (e.g. a booking id or user id). */
    @Column(name = "subject_id", length = 64)
    private String subjectId;

    /** Optional display name of the holder (shown on the scanner result screen). */
    @Column(name = "name", length = 150)
    private String name;

    /** Optional holder email. */
    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "status", nullable = false, length = 20)
    private String status = STATUS_ACTIVE;

    @Column(name = "issued_at", nullable = false)
    private Instant issuedAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "used_at")
    private Instant usedAt;

    @Column(name = "used_by", length = 150)
    private String usedBy;

    /** How many times the pass may be consumed (1 = one-time). */
    @Column(name = "max_uses", nullable = false)
    private int maxUses = 1;

    /** How many times it has been consumed so far. */
    @Column(name = "use_count", nullable = false)
    private int useCount = 0;

    public QrPass() {
    }

    // Getters / setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getJti() {
        return jti;
    }

    public void setJti(String jti) {
        this.jti = jti;
    }

    public String getSubjectType() {
        return subjectType;
    }

    public void setSubjectType(String subjectType) {
        this.subjectType = subjectType;
    }

    public String getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(String subjectId) {
        this.subjectId = subjectId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getIssuedAt() {
        return issuedAt;
    }

    public void setIssuedAt(Instant issuedAt) {
        this.issuedAt = issuedAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Instant getUsedAt() {
        return usedAt;
    }

    public void setUsedAt(Instant usedAt) {
        this.usedAt = usedAt;
    }

    public String getUsedBy() {
        return usedBy;
    }

    public void setUsedBy(String usedBy) {
        this.usedBy = usedBy;
    }

    public int getMaxUses() {
        return maxUses;
    }

    public void setMaxUses(int maxUses) {
        this.maxUses = maxUses;
    }

    public int getUseCount() {
        return useCount;
    }

    public void setUseCount(int useCount) {
        this.useCount = useCount;
    }

    // Convenience

    @Transient
    public boolean isRevoked() {
        return STATUS_REVOKED.equals(status);
    }

    @Transient
    public boolean isExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }

    @Transient
    public boolean isUsedUp() {
        return useCount >= maxUses;
    }
}
