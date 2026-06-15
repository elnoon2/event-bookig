package com.badyauniversity.eventbooking.qrpass.dto;

import java.time.Instant;

/**
 * Result of issuing a pass: the signed payload to encode in a QR, a ready-to-render PNG data URL,
 * and the metadata the caller needs to display or store.
 */
public class IssuePassResponse {

    private String jti;
    /** The exact text to encode in the QR code (signed; safe to print). */
    private String qrPayload;
    /** data:image/png;base64,... rendering of {@link #qrPayload}, for direct <img src> use. */
    private String qrImageDataUrl;
    private Instant expiresAt;

    public IssuePassResponse() {
    }

    public IssuePassResponse(String jti, String qrPayload, String qrImageDataUrl, Instant expiresAt) {
        this.jti = jti;
        this.qrPayload = qrPayload;
        this.qrImageDataUrl = qrImageDataUrl;
        this.expiresAt = expiresAt;
    }

    public String getJti() {
        return jti;
    }

    public void setJti(String jti) {
        this.jti = jti;
    }

    public String getQrPayload() {
        return qrPayload;
    }

    public void setQrPayload(String qrPayload) {
        this.qrPayload = qrPayload;
    }

    public String getQrImageDataUrl() {
        return qrImageDataUrl;
    }

    public void setQrImageDataUrl(String qrImageDataUrl) {
        this.qrImageDataUrl = qrImageDataUrl;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }
}
