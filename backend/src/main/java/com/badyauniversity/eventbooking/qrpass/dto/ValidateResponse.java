package com.badyauniversity.eventbooking.qrpass.dto;

import com.badyauniversity.eventbooking.qrpass.model.QrPass;
import com.badyauniversity.eventbooking.qrpass.model.ValidationStatus;

import java.time.Instant;

/**
 * Validation outcome returned to the scanner. {@link #status} drives the result screen; holder
 * details are included only when the pass was found (never reflected from the untrusted QR payload).
 */
public class ValidateResponse {

    private ValidationStatus status;
    private String message;
    private String subjectType;
    private String subjectId;
    private String name;
    private String email;
    private Instant expiresAt;
    private Instant usedAt;

    public ValidateResponse() {
    }

    public ValidateResponse(ValidationStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    /** Build a response and copy the (server-side) holder details from the pass row. */
    public static ValidateResponse from(ValidationStatus status, String message, QrPass pass) {
        ValidateResponse r = new ValidateResponse(status, message);
        if (pass != null) {
            r.subjectType = pass.getSubjectType();
            r.subjectId = pass.getSubjectId();
            r.name = pass.getName();
            r.email = pass.getEmail();
            r.expiresAt = pass.getExpiresAt();
            r.usedAt = pass.getUsedAt();
        }
        return r;
    }

    public ValidationStatus getStatus() {
        return status;
    }

    public void setStatus(ValidationStatus status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
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
}
