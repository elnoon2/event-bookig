package com.badyauniversity.eventbooking.qrpass.web;

import com.badyauniversity.eventbooking.qrpass.config.QrRateLimitFilter;
import com.badyauniversity.eventbooking.qrpass.dto.IssuePassRequest;
import com.badyauniversity.eventbooking.qrpass.dto.IssuePassResponse;
import com.badyauniversity.eventbooking.qrpass.dto.ValidateRequest;
import com.badyauniversity.eventbooking.qrpass.dto.ValidateResponse;
import com.badyauniversity.eventbooking.qrpass.model.QrPass;
import com.badyauniversity.eventbooking.qrpass.model.QrScanAudit;
import com.badyauniversity.eventbooking.qrpass.repository.QrScanAuditRepository;
import com.badyauniversity.eventbooking.qrpass.service.QrPassService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST API for the signed QR pass module.
 *
 *  POST /api/qr-pass/issue          issue a signed pass (returns payload + QR image)
 *  POST /api/qr-pass/validate       validate a scanned payload -> { status, ... } (rate-limited, audited)
 *  POST /api/qr-pass/{jti}/revoke   revoke a pass
 *  GET  /api/qr-pass/{jti}          current status of a pass
 *  GET  /api/qr-pass/audit          recent scan attempts (audit trail)
 */
@RestController
@RequestMapping("/api/qr-pass")
public class QrPassController {

    private final QrPassService qrPassService;
    private final QrScanAuditRepository auditRepository;

    public QrPassController(QrPassService qrPassService, QrScanAuditRepository auditRepository) {
        this.qrPassService = qrPassService;
        this.auditRepository = auditRepository;
    }

    @PostMapping("/issue")
    public ResponseEntity<IssuePassResponse> issue(@RequestBody(required = false) IssuePassRequest request) {
        return ResponseEntity.ok(qrPassService.issue(request != null ? request : new IssuePassRequest()));
    }

    @PostMapping("/validate")
    public ResponseEntity<ValidateResponse> validate(@RequestBody ValidateRequest request,
                                                     HttpServletRequest http) {
        String ip = QrRateLimitFilter.clientIp(http);
        String ua = http.getHeader("User-Agent");
        ValidateResponse result = qrPassService.validate(
                request != null ? request.getPayload() : null,
                ip, ua, request != null ? request.getScannedBy() : null);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{jti}/revoke")
    public ResponseEntity<Map<String, Object>> revoke(@PathVariable String jti,
                                                      @RequestBody(required = false) Map<String, String> body) {
        String by = body != null ? body.get("revokedBy") : null;
        boolean ok = qrPassService.revoke(jti, by);
        Map<String, Object> resp = new HashMap<>();
        resp.put("jti", jti);
        resp.put("revoked", ok);
        if (!ok) {
            resp.put("message", "Pass not found.");
            return ResponseEntity.status(404).body(resp);
        }
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/{jti}")
    public ResponseEntity<Map<String, Object>> status(@PathVariable String jti) {
        return qrPassService.getPass(jti)
                .<ResponseEntity<Map<String, Object>>>map(pass -> {
                    Map<String, Object> resp = new HashMap<>();
                    resp.put("jti", pass.getJti());
                    resp.put("status", qrPassService.effectiveStatus(pass));
                    resp.put("subjectType", pass.getSubjectType());
                    resp.put("subjectId", pass.getSubjectId());
                    resp.put("name", pass.getName());
                    resp.put("issuedAt", pass.getIssuedAt());
                    resp.put("expiresAt", pass.getExpiresAt());
                    resp.put("usedAt", pass.getUsedAt());
                    resp.put("useCount", pass.getUseCount());
                    resp.put("maxUses", pass.getMaxUses());
                    return ResponseEntity.ok(resp);
                })
                .orElseGet(() -> ResponseEntity.status(404).body(Map.of("message", "Pass not found.")));
    }

    @GetMapping("/audit")
    public ResponseEntity<List<QrScanAudit>> audit() {
        return ResponseEntity.ok(auditRepository.findTop100ByOrderByIdDesc());
    }
}
