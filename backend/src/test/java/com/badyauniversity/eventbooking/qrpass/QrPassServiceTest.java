package com.badyauniversity.eventbooking.qrpass;

import com.badyauniversity.eventbooking.qrpass.dto.IssuePassRequest;
import com.badyauniversity.eventbooking.qrpass.dto.IssuePassResponse;
import com.badyauniversity.eventbooking.qrpass.dto.ValidateResponse;
import com.badyauniversity.eventbooking.qrpass.model.QrPass;
import com.badyauniversity.eventbooking.qrpass.model.ValidationStatus;
import com.badyauniversity.eventbooking.qrpass.repository.QrPassRepository;
import com.badyauniversity.eventbooking.qrpass.repository.QrScanAuditRepository;
import com.badyauniversity.eventbooking.qrpass.service.QrPassService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class QrPassServiceTest {

    @Autowired QrPassService service;
    @Autowired QrPassRepository passRepository;
    @Autowired QrScanAuditRepository auditRepository;

    @BeforeEach
    void clean() {
        auditRepository.deleteAll();
        passRepository.deleteAll();
    }

    private IssuePassResponse issue(long ttlSeconds, int maxUses) {
        IssuePassRequest req = new IssuePassRequest();
        req.setName("Test Holder");
        req.setSubjectType("ACCESS");
        req.setTtlSeconds(ttlSeconds);
        req.setMaxUses(maxUses);
        return service.issue(req);
    }

    @Test
    void validPassIsAcceptedThenReportsAlreadyUsed() {
        IssuePassResponse issued = issue(3600, 1);
        long auditBefore = auditRepository.count();

        ValidateResponse first = service.validate(issued.getQrPayload(), "1.1.1.1", "test", "gate-1");
        assertEquals(ValidationStatus.VALID, first.getStatus());
        assertEquals("Test Holder", first.getName());

        // Replay the same QR -> already used.
        ValidateResponse second = service.validate(issued.getQrPayload(), "1.1.1.1", "test", "gate-1");
        assertEquals(ValidationStatus.ALREADY_USED, second.getStatus());

        // Two attempts -> two audit rows.
        assertEquals(auditBefore + 2, auditRepository.count());
    }

    @Test
    void tamperedSignatureIsInvalid() {
        IssuePassResponse issued = issue(3600, 1);
        String p = issued.getQrPayload();
        // Flip the last character of the signature segment.
        char last = p.charAt(p.length() - 1);
        String tampered = p.substring(0, p.length() - 1) + (last == 'A' ? 'B' : 'A');

        ValidateResponse r = service.validate(tampered, "1.1.1.1", "test", "gate-1");
        assertEquals(ValidationStatus.INVALID, r.getStatus());
        // The forged pass is not consumed.
        assertEquals(0, passRepository.findByJti(issued.getJti()).orElseThrow().getUseCount());
    }

    @Test
    void expiredPassIsRejected() {
        IssuePassResponse issued = issue(3600, 1);
        QrPass pass = passRepository.findByJti(issued.getJti()).orElseThrow();
        pass.setExpiresAt(Instant.now().minusSeconds(60));
        passRepository.save(pass);

        ValidateResponse r = service.validate(issued.getQrPayload(), "1.1.1.1", "test", "gate-1");
        assertEquals(ValidationStatus.EXPIRED, r.getStatus());
    }

    @Test
    void revokedPassIsRejected() {
        IssuePassResponse issued = issue(3600, 1);
        assertTrue(service.revoke(issued.getJti(), "admin"));

        ValidateResponse r = service.validate(issued.getQrPayload(), "1.1.1.1", "test", "gate-1");
        assertEquals(ValidationStatus.REVOKED, r.getStatus());
    }

    @Test
    void multiUsePassAllowsConfiguredUses() {
        IssuePassResponse issued = issue(3600, 2);
        assertEquals(ValidationStatus.VALID, service.validate(issued.getQrPayload(), "ip", "ua", "g").getStatus());
        assertEquals(ValidationStatus.VALID, service.validate(issued.getQrPayload(), "ip", "ua", "g").getStatus());
        assertEquals(ValidationStatus.ALREADY_USED, service.validate(issued.getQrPayload(), "ip", "ua", "g").getStatus());
    }
}
