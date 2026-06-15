package com.badyauniversity.eventbooking.qrpass.dto;

/**
 * Request to issue a new signed QR pass. All fields are optional except that sensible defaults are
 * applied server-side (TTL and max-uses come from configuration when not supplied).
 */
public class IssuePassRequest {

    private String subjectType;
    private String subjectId;
    private String name;
    private String email;
    /** Time-to-live in seconds; null/0 -> server default. */
    private Long ttlSeconds;
    /** Allowed number of uses; null/0 -> server default (1 = one-time). */
    private Integer maxUses;

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

    public Long getTtlSeconds() {
        return ttlSeconds;
    }

    public void setTtlSeconds(Long ttlSeconds) {
        this.ttlSeconds = ttlSeconds;
    }

    public Integer getMaxUses() {
        return maxUses;
    }

    public void setMaxUses(Integer maxUses) {
        this.maxUses = maxUses;
    }
}
