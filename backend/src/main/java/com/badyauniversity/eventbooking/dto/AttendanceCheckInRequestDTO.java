package com.badyauniversity.eventbooking.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class AttendanceCheckInRequestDTO {

    @NotNull(message = "Event id is required")
    private Long eventId;

    @NotBlank(message = "QR token is required")
    private String qrToken;

    public Long getEventId() {
        return eventId;
    }

    public void setEventId(Long eventId) {
        this.eventId = eventId;
    }

    public String getQrToken() {
        return qrToken;
    }

    public void setQrToken(String qrToken) {
        this.qrToken = qrToken != null ? qrToken.trim() : null;
    }
}
