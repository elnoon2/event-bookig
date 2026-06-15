package com.badyauniversity.eventbooking.qrpass.service;

import com.badyauniversity.eventbooking.qrpass.config.QrPassProperties;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

/**
 * Builds and verifies the signed QR payload.
 *
 * Format: {@code BADYAQR1.<base64url(jti)>.<base64url(HMAC_SHA256(secret, jti))>}
 *
 * The signature binds the token id to the server's secret key, so a forged or edited QR fails
 * verification without ever touching the database. Verification is constant-time to avoid leaking
 * the expected signature via timing.
 */
@Component
public class QrTokenSigner {

    private static final String PREFIX = "BADYAQR1";
    private static final String HMAC_ALGO = "HmacSHA256";
    private static final Base64.Encoder B64 = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder B64D = Base64.getUrlDecoder();

    private final QrPassProperties props;

    public QrTokenSigner(QrPassProperties props) {
        this.props = props;
    }

    /** Produce the signed payload text to encode into a QR for the given token id. */
    public String buildPayload(String jti) {
        String sig = B64.encodeToString(hmac(jti));
        String jtiPart = B64.encodeToString(jti.getBytes(StandardCharsets.UTF_8));
        return PREFIX + "." + jtiPart + "." + sig;
    }

    /**
     * Verify a scanned payload and return the embedded token id, or {@code null} if the payload is
     * missing, malformed, or its signature does not verify. Never throws.
     */
    public String verifyAndExtractJti(String payload) {
        if (payload == null) {
            return null;
        }
        String[] parts = payload.trim().split("\\.");
        if (parts.length != 3 || !PREFIX.equals(parts[0])) {
            return null;
        }
        final String jti;
        final byte[] providedSig;
        try {
            jti = new String(B64D.decode(parts[1]), StandardCharsets.UTF_8);
            providedSig = B64D.decode(parts[2]);
        } catch (IllegalArgumentException e) {
            return null;
        }
        byte[] expectedSig = hmac(jti);
        // Constant-time comparison.
        if (!MessageDigest.isEqual(expectedSig, providedSig)) {
            return null;
        }
        return jti;
    }

    private byte[] hmac(String data) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGO);
            mac.init(new SecretKeySpec(props.getSigningSecret().getBytes(StandardCharsets.UTF_8), HMAC_ALGO));
            return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to compute HMAC for QR pass", e);
        }
    }
}
