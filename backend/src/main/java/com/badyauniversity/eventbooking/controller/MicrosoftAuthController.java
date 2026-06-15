package com.badyauniversity.eventbooking.controller;

import com.badyauniversity.eventbooking.config.MicrosoftOAuthProperties;
import com.badyauniversity.eventbooking.model.User;
import com.badyauniversity.eventbooking.service.MicrosoftAuthException;
import com.badyauniversity.eventbooking.service.MicrosoftAuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * "Sign in with Microsoft" endpoints (manual OAuth2 Authorization Code + PKCE flow).
 *
 *  GET /api/auth/microsoft/login        -> redirects the browser to Microsoft
 *  GET /api/auth/microsoft/callback     -> Microsoft redirects back here; provisions the user
 *  GET /api/auth/microsoft/session-user -> one-time read of the signed-in user (frontend handoff)
 *  GET /api/auth/microsoft/status       -> reports whether the feature is configured
 *
 * The login result is stashed in the HTTP session (no PII in the redirect URL) and the frontend
 * reads it once via /session-user, then stores it in localStorage exactly like password login.
 */
@Controller
@RequestMapping("/api/auth/microsoft")
public class MicrosoftAuthController {

    private static final String SESSION_STATE = "ms_oauth_state";
    private static final String SESSION_NONCE = "ms_oauth_nonce";
    private static final String SESSION_VERIFIER = "ms_oauth_verifier";
    private static final String SESSION_USER = "ms_oauth_user";

    private final MicrosoftAuthService microsoftAuthService;
    private final MicrosoftOAuthProperties props;

    public MicrosoftAuthController(MicrosoftAuthService microsoftAuthService, MicrosoftOAuthProperties props) {
        this.microsoftAuthService = microsoftAuthService;
        this.props = props;
    }

    @GetMapping("/login")
    public void login(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!microsoftAuthService.isEnabled()) {
            response.sendRedirect(errorRedirect("not_configured"));
            return;
        }

        String state = MicrosoftAuthService.randomUrlToken(32);
        String nonce = MicrosoftAuthService.randomUrlToken(32);
        String verifier = MicrosoftAuthService.randomUrlToken(48);
        String challenge = MicrosoftAuthService.codeChallengeFor(verifier);

        HttpSession session = request.getSession(true);
        session.setAttribute(SESSION_STATE, state);
        session.setAttribute(SESSION_NONCE, nonce);
        session.setAttribute(SESSION_VERIFIER, verifier);

        response.sendRedirect(microsoftAuthService.buildAuthorizationUrl(state, nonce, challenge));
    }

    @GetMapping("/callback")
    public void callback(@RequestParam(required = false) String code,
                         @RequestParam(required = false) String state,
                         @RequestParam(name = "error", required = false) String error,
                         @RequestParam(name = "error_description", required = false) String errorDescription,
                         HttpServletRequest request,
                         HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession(false);
        try {
            if (error != null && !error.isBlank()) {
                // The user denied consent or Microsoft reported an error.
                throw new MicrosoftAuthException(error, errorDescription != null ? errorDescription : error);
            }
            if (session == null) {
                throw new MicrosoftAuthException("session_expired", "Your login session expired. Please try again.");
            }

            String savedState = (String) session.getAttribute(SESSION_STATE);
            String nonce = (String) session.getAttribute(SESSION_NONCE);
            String verifier = (String) session.getAttribute(SESSION_VERIFIER);

            // Single-use: clear the flow attributes immediately.
            session.removeAttribute(SESSION_STATE);
            session.removeAttribute(SESSION_NONCE);
            session.removeAttribute(SESSION_VERIFIER);

            if (code == null || code.isBlank() || savedState == null
                    || !MicrosoftAuthService.constantTimeEquals(savedState, state)) {
                throw new MicrosoftAuthException("state_mismatch", "Invalid OAuth state. Please try signing in again.");
            }

            User user = microsoftAuthService.completeLogin(code, verifier, nonce);
            session.setAttribute(SESSION_USER, buildUserResponse(user));
            response.sendRedirect(successRedirect());
        } catch (MicrosoftAuthException e) {
            System.err.println("Microsoft login failed [" + e.getCode() + "]: " + e.getMessage());
            response.sendRedirect(errorRedirect(e.getCode()));
        } catch (Exception e) {
            System.err.println("Microsoft login failed [server_error]: " + e.getMessage());
            response.sendRedirect(errorRedirect("server_error"));
        }
    }

    @GetMapping("/session-user")
    @ResponseBody
    public ResponseEntity<?> sessionUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        Object user = session == null ? null : session.getAttribute(SESSION_USER);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "No active Microsoft login session."));
        }
        // One-time read so a stale session can't replay the identity.
        session.removeAttribute(SESSION_USER);
        return ResponseEntity.ok(user);
    }

    @GetMapping("/status")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> status() {
        Map<String, Object> body = new HashMap<>();
        body.put("enabled", microsoftAuthService.isEnabled());
        return ResponseEntity.ok(body);
    }

    /** Mirrors UserController.authenticateUser's response so the frontend treats it identically. */
    private Map<String, Object> buildUserResponse(User user) {
        Map<String, Object> response = new HashMap<>();
        response.put("id", user.getId());
        response.put("name", user.getName());
        response.put("email", user.getEmail());
        response.put("phone", user.getPhone());
        response.put("faculty", user.getFaculty());
        response.put("studentId", user.getStudentId());
        response.put("role", user.getRole());
        response.put("qrToken", user.getQrToken());
        response.put("qrImageBase64", user.getQrImageBase64());
        response.put("photoUrl", user.getProfilePhotoBase64());
        response.put("authProvider", "microsoft");
        return response;
    }

    private String successRedirect() {
        return props.getPostLoginRedirect() + "?msauth=1";
    }

    private String errorRedirect(String code) {
        return props.getPostLoginRedirect() + "?msauth_error="
                + URLEncoder.encode(code, StandardCharsets.UTF_8);
    }
}
