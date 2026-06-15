package com.badyauniversity.eventbooking.qrpass.dto;

/**
 * Request to validate a scanned QR. {@code payload} is the raw decoded text from the QR code;
 * {@code scannedBy} optionally identifies the operator/gate for the audit log.
 */
public class ValidateRequest {

    private String payload;
    private String scannedBy;

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public String getScannedBy() {
        return scannedBy;
    }

    public void setScannedBy(String scannedBy) {
        this.scannedBy = scannedBy;
    }
}
