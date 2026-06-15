package com.badyauniversity.eventbooking.qrpass.config;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration for the signed QR pass module, bound from {@code app.qrpass.*}.
 * The signing secret is the HMAC key used to sign/verify every pass — keep it secret and rotate it
 * if leaked. Provide it via the QR_SIGNING_SECRET environment variable in real environments.
 */
@Component
@ConfigurationProperties(prefix = "app.qrpass")
public class QrPassProperties {

    /** HMAC-SHA256 key. The "dev-insecure-change-me" default triggers a startup warning. */
    private String signingSecret = "dev-insecure-change-me";

    /** Default pass lifetime in seconds when the caller doesn't specify one. */
    private long defaultTtlSeconds = 86_400; // 24h

    /** Default number of allowed uses (1 = one-time). */
    private int defaultMaxUses = 1;

    /** Max validate requests allowed per client IP within the rate-limit window. */
    private int rateLimitPerMinute = 60;

    public boolean isUsingInsecureDefaultSecret() {
        return "dev-insecure-change-me".equals(signingSecret);
    }

    @PostConstruct
    void warnIfInsecure() {
        if (isUsingInsecureDefaultSecret()) {
            System.err.println("============================================================");
            System.err.println("SECURITY WARNING: QR pass signing is using the default key.");
            System.err.println("Set the QR_SIGNING_SECRET environment variable before deploying.");
            System.err.println("============================================================");
        }
    }

    public String getSigningSecret() {
        return signingSecret;
    }

    public void setSigningSecret(String signingSecret) {
        this.signingSecret = signingSecret;
    }

    public long getDefaultTtlSeconds() {
        return defaultTtlSeconds;
    }

    public void setDefaultTtlSeconds(long defaultTtlSeconds) {
        this.defaultTtlSeconds = defaultTtlSeconds;
    }

    public int getDefaultMaxUses() {
        return defaultMaxUses;
    }

    public void setDefaultMaxUses(int defaultMaxUses) {
        this.defaultMaxUses = defaultMaxUses;
    }

    public int getRateLimitPerMinute() {
        return rateLimitPerMinute;
    }

    public void setRateLimitPerMinute(int rateLimitPerMinute) {
        this.rateLimitPerMinute = rateLimitPerMinute;
    }
}
