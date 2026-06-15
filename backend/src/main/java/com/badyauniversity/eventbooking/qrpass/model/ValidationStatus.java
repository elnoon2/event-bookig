package com.badyauniversity.eventbooking.qrpass.model;

/**
 * Result of validating a scanned QR pass. Returned to the scanner and recorded in the audit log.
 */
public enum ValidationStatus {
    /** Signature verified, pass exists, not expired/used/revoked — and it was just consumed. */
    VALID,
    /** Missing/malformed payload or the HMAC signature did not verify (forged or corrupt QR). */
    INVALID,
    /** The pass exists but its expiry time has passed. */
    EXPIRED,
    /** The pass exists but has already been used up (one-time pass already consumed). */
    ALREADY_USED,
    /** The pass was explicitly revoked by an administrator. */
    REVOKED
}
