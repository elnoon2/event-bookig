package com.badyauniversity.eventbooking.service;

/**
 * Raised when the Microsoft OAuth flow fails. The {@link #code} is a short, safe machine token
 * (e.g. "state_mismatch", "domain_not_allowed") suitable for surfacing to the frontend as a query
 * parameter; the message carries detail for server logs.
 */
public class MicrosoftAuthException extends RuntimeException {

    private final String code;

    public MicrosoftAuthException(String code, String message) {
        super(message);
        this.code = code == null || code.isBlank() ? "microsoft_login_failed" : code;
    }

    public String getCode() {
        return code;
    }
}
