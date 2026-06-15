package com.badyauniversity.eventbooking.qrpass.service;

import com.badyauniversity.eventbooking.qrpass.config.QrPassProperties;
import com.badyauniversity.eventbooking.qrpass.dto.IssuePassRequest;
import com.badyauniversity.eventbooking.qrpass.dto.IssuePassResponse;
import com.badyauniversity.eventbooking.qrpass.dto.ValidateResponse;
import com.badyauniversity.eventbooking.qrpass.model.QrPass;
import com.badyauniversity.eventbooking.qrpass.model.QrScanAudit;
import com.badyauniversity.eventbooking.qrpass.model.ValidationStatus;
import com.badyauniversity.eventbooking.qrpass.repository.QrPassRepository;
import com.badyauniversity.eventbooking.qrpass.repository.QrScanAuditRepository;
import com.badyauniversity.eventbooking.service.QrCodeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Core QR pass logic: issue signed passes, validate scanned payloads against the database, revoke,
 * and query status. Every validation attempt is audited. Validation runs in a transaction with a row
 * lock so a one-time pass cannot be consumed twice by concurrent scans.
 */
@Service
public class QrPassService {

    private final QrPassRepository passRepository;
    private final QrScanAuditRepository auditRepository;
    private final QrTokenSigner signer;
    private final QrCodeService qrCodeService;
    private final QrPassProperties props;

    public QrPassService(QrPassRepository passRepository,
                         QrScanAuditRepository auditRepository,
                         QrTokenSigner signer,
                         QrCodeService qrCodeService,
                         QrPassProperties props) {
        this.passRepository = passRepository;
        this.auditRepository = auditRepository;
        this.signer = signer;
        this.qrCodeService = qrCodeService;
        this.props = props;
    }

    /** Issue a brand-new signed pass and render its QR image. */
    @Transactional
    public IssuePassResponse issue(IssuePassRequest req) {
        long ttl = (req.getTtlSeconds() != null && req.getTtlSeconds() > 0)
                ? req.getTtlSeconds() : props.getDefaultTtlSeconds();
        int maxUses = (req.getMaxUses() != null && req.getMaxUses() > 0)
                ? req.getMaxUses() : props.getDefaultMaxUses();

        QrPass pass = new QrPass();
        pass.setJti(UUID.randomUUID().toString());
        pass.setSubjectType(req.getSubjectType());
        pass.setSubjectId(req.getSubjectId());
        pass.setName(req.getName());
        pass.setEmail(req.getEmail());
        pass.setStatus(QrPass.STATUS_ACTIVE);
        pass.setIssuedAt(Instant.now());
        pass.setExpiresAt(Instant.now().plusSeconds(ttl));
        pass.setMaxUses(maxUses);
        pass.setUseCount(0);
        passRepository.save(pass);

        String payload = signer.buildPayload(pass.getJti());
        String imageDataUrl = qrCodeService.encodeToPngDataUrl(payload);
        return new IssuePassResponse(pass.getJti(), payload, imageDataUrl, pass.getExpiresAt());
    }

    /**
     * Validate a scanned payload and (if valid) consume one use. Always returns a status; never
     * throws for business outcomes. Writes exactly one audit row per call.
     */
    @Transactional
    public ValidateResponse validate(String payload, String ip, String userAgent, String scannedBy) {
        // 1. Verify the signature first — a forged/edited QR is rejected without a DB hit.
        String jti = signer.verifyAndExtractJti(payload);
        if (jti == null) {
            audit(null, ValidationStatus.INVALID, ip, userAgent, scannedBy);
            return new ValidateResponse(ValidationStatus.INVALID, "Invalid or unsigned QR code.");
        }

        // 2. Look up the authoritative pass row (locked for the duration of the transaction).
        Optional<QrPass> found = passRepository.findByJtiForUpdate(jti);
        if (found.isEmpty()) {
            audit(jti, ValidationStatus.INVALID, ip, userAgent, scannedBy);
            return new ValidateResponse(ValidationStatus.INVALID, "Unknown QR code.");
        }
        QrPass pass = found.get();

        // 3. State checks, most-specific first.
        if (pass.isRevoked()) {
            audit(jti, ValidationStatus.REVOKED, ip, userAgent, scannedBy);
            return ValidateResponse.from(ValidationStatus.REVOKED, "This pass has been revoked.", pass);
        }
        if (pass.isExpired()) {
            audit(jti, ValidationStatus.EXPIRED, ip, userAgent, scannedBy);
            return ValidateResponse.from(ValidationStatus.EXPIRED, "This pass has expired.", pass);
        }
        if (pass.isUsedUp()) {
            audit(jti, ValidationStatus.ALREADY_USED, ip, userAgent, scannedBy);
            return ValidateResponse.from(ValidationStatus.ALREADY_USED, "This pass has already been used.", pass);
        }

        // 4. Consume one use.
        pass.setUseCount(pass.getUseCount() + 1);
        pass.setUsedAt(Instant.now());
        pass.setUsedBy(scannedBy);
        if (pass.isUsedUp()) {
            pass.setStatus(QrPass.STATUS_USED);
        }
        passRepository.save(pass);

        audit(jti, ValidationStatus.VALID, ip, userAgent, scannedBy);
        return ValidateResponse.from(ValidationStatus.VALID, "Pass verified.", pass);
    }

    /** Revoke a pass so all future scans return REVOKED. Idempotent. */
    @Transactional
    public boolean revoke(String jti, String revokedBy) {
        Optional<QrPass> found = passRepository.findByJtiForUpdate(jti);
        if (found.isEmpty()) {
            return false;
        }
        QrPass pass = found.get();
        pass.setStatus(QrPass.STATUS_REVOKED);
        if (pass.getUsedBy() == null) {
            pass.setUsedBy(revokedBy);
        }
        passRepository.save(pass);
        return true;
    }

    @Transactional(readOnly = true)
    public Optional<QrPass> getPass(String jti) {
        return passRepository.findByJti(jti);
    }

    /** The effective, human-readable status of a pass (accounts for expiry). */
    public String effectiveStatus(QrPass pass) {
        if (pass.isRevoked()) {
            return QrPass.STATUS_REVOKED;
        }
        if (pass.isExpired()) {
            return "EXPIRED";
        }
        if (pass.isUsedUp()) {
            return QrPass.STATUS_USED;
        }
        return QrPass.STATUS_ACTIVE;
    }

    private void audit(String jti, ValidationStatus result, String ip, String userAgent, String scannedBy) {
        auditRepository.save(new QrScanAudit(jti, result, ip, userAgent, scannedBy));
    }
}
