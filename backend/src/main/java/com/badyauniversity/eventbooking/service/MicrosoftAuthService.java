package com.badyauniversity.eventbooking.service;

import com.badyauniversity.eventbooking.config.MicrosoftOAuthProperties;
import com.badyauniversity.eventbooking.model.User;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.RemoteJWKSet;
import com.nimbusds.jose.proc.JWSKeySelector;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTClaimsVerifier;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * Implements the Microsoft (Entra ID) OAuth2 Authorization Code + PKCE flow:
 *  - builds the authorize URL,
 *  - exchanges the authorization code for tokens server-to-server (client secret over TLS),
 *  - validates the OpenID Connect ID token (RS256 signature via JWKS, aud, exp, iss, nonce),
 *  - enforces the allowed email domain, and provisions/looks up the local user.
 *
 * It deliberately avoids the Spring Security filter chain so the existing email/password
 * authentication keeps working untouched.
 */
@Service
public class MicrosoftAuthService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String GRAPH_BASE = "https://graph.microsoft.com/v1.0";

    private final MicrosoftOAuthProperties props;
    private final UserService userService;
    private final RestTemplate restTemplate = new RestTemplate();

    /** Lazily built and cached; the underlying JWKS keys are themselves cached/refreshed by Nimbus. */
    private volatile ConfigurableJWTProcessor<SecurityContext> jwtProcessor;

    public MicrosoftAuthService(MicrosoftOAuthProperties props, UserService userService) {
        this.props = props;
        this.userService = userService;
    }

    public boolean isEnabled() {
        return props.isEnabled();
    }

    public String getPostLoginRedirect() {
        return props.getPostLoginRedirect();
    }

    private String authority() {
        String tenant = props.getTenantId();
        if (tenant == null || tenant.isBlank()) {
            tenant = "organizations";
        }
        return "https://login.microsoftonline.com/" + tenant.trim();
    }

    // ---------------------------------------------------------------------
    // Step 1 — build the authorization request
    // ---------------------------------------------------------------------

    public String buildAuthorizationUrl(String state, String nonce, String codeChallenge) {
        return UriComponentsBuilder.fromHttpUrl(authority() + "/oauth2/v2.0/authorize")
                .queryParam("client_id", props.getClientId())
                .queryParam("response_type", "code")
                .queryParam("redirect_uri", props.getRedirectUri())
                .queryParam("response_mode", "query")
                .queryParam("scope", props.getScopes())
                .queryParam("state", state)
                .queryParam("nonce", nonce)
                .queryParam("code_challenge", codeChallenge)
                .queryParam("code_challenge_method", "S256")
                .encode(StandardCharsets.UTF_8)
                .build()
                .toUriString();
    }

    // ---------------------------------------------------------------------
    // Step 2 — handle the callback: exchange + validate + provision
    // ---------------------------------------------------------------------

    public User completeLogin(String code, String codeVerifier, String expectedNonce) {
        TokenResponse tokens = exchangeCodeForTokens(code, codeVerifier);
        JWTClaimsSet claims = validateIdToken(tokens.idToken(), expectedNonce);

        String oid = stringClaim(claims, "oid");
        if (oid == null) {
            oid = claims.getSubject();
        }
        String email = firstNonBlank(
                stringClaim(claims, "preferred_username"),
                stringClaim(claims, "email"),
                stringClaim(claims, "upn"));
        String name = stringClaim(claims, "name");

        // Retrieve richer profile + photo from Microsoft Graph using the access token (best-effort).
        String faculty = null;
        String phone = null;
        String studentId = null;
        GraphProfile profile = fetchGraphProfile(tokens.accessToken());
        if (profile != null) {
            email = firstNonBlank(email, profile.email());
            name = firstNonBlank(name, profile.displayName());
            faculty = profile.department();   // university "department" = faculty/major
            phone = profile.phone();          // personal mobile (often empty in uni directories)
            studentId = profile.employeeId(); // university "employeeId" = student number
        }
        String photoDataUrl = fetchGraphPhoto(tokens.accessToken());

        if (oid == null || oid.isBlank()) {
            throw new MicrosoftAuthException("invalid_token", "Microsoft token did not contain a stable user id.");
        }
        if (email == null || email.isBlank()) {
            throw new MicrosoftAuthException("no_email", "The Microsoft account did not expose an email address.");
        }

        String domain = props.getAllowedEmailDomain();
        if (domain != null && !domain.isBlank()
                && !email.toLowerCase().endsWith("@" + domain.trim().toLowerCase())) {
            throw new MicrosoftAuthException("domain_not_allowed",
                    "Only " + domain + " accounts may sign in with Microsoft (got " + email + ").");
        }

        return userService.provisionMicrosoftUser(oid, email, name, photoDataUrl, faculty, phone, studentId);
    }

    private TokenResponse exchangeCodeForTokens(String code, String codeVerifier) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", props.getClientId());
        form.add("scope", props.getScopes());
        form.add("code", code);
        form.add("redirect_uri", props.getRedirectUri());
        form.add("grant_type", "authorization_code");
        form.add("code_verifier", codeVerifier);
        form.add("client_secret", props.getClientSecret());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(form, headers);

        try {
            ResponseEntity<Map> response =
                    restTemplate.postForEntity(authority() + "/oauth2/v2.0/token", request, Map.class);
            Map body = response.getBody();
            Object idToken = body == null ? null : body.get("id_token");
            Object accessToken = body == null ? null : body.get("access_token");
            if (idToken == null) {
                throw new MicrosoftAuthException("token_exchange_failed", "Microsoft did not return an id_token.");
            }
            return new TokenResponse(idToken.toString(), accessToken == null ? null : accessToken.toString());
        } catch (RestClientException e) {
            throw new MicrosoftAuthException("token_exchange_failed",
                    "Failed to exchange the authorization code with Microsoft: " + e.getMessage());
        }
    }

    // ---------------------------------------------------------------------
    // Microsoft Graph — profile + photo (best-effort; never fail login on these)
    // ---------------------------------------------------------------------

    private GraphProfile fetchGraphProfile(String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            return null;
        }
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            // Ask Graph for the fields we map onto the local profile.
            String url = GRAPH_BASE + "/me?$select=displayName,mail,userPrincipalName,department,"
                    + "jobTitle,mobilePhone,employeeId";
            ResponseEntity<Map> response = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(headers), Map.class);
            Map body = response.getBody();
            if (body == null) {
                return null;
            }
            String displayName = asString(body.get("displayName"));
            String email = firstNonBlank(asString(body.get("mail")), asString(body.get("userPrincipalName")));
            String department = asString(body.get("department"));   // university faculty/major
            String phone = asString(body.get("mobilePhone"));       // personal mobile (often empty)
            String employeeId = asString(body.get("employeeId"));   // university student/staff number
            return new GraphProfile(displayName, email, department, phone, employeeId);
        } catch (Exception e) {
            System.err.println("Graph /me lookup failed (continuing): " + e.getMessage());
            return null;
        }
    }

    private String fetchGraphPhoto(String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            return null;
        }
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            headers.setAccept(List.of(MediaType.IMAGE_JPEG, MediaType.IMAGE_PNG, MediaType.ALL));
            ResponseEntity<byte[]> response = restTemplate.exchange(
                    GRAPH_BASE + "/me/photo/$value", HttpMethod.GET, new HttpEntity<>(headers), byte[].class);
            byte[] bytes = response.getBody();
            if (bytes == null || bytes.length == 0) {
                return null;
            }
            MediaType contentType = response.getHeaders().getContentType();
            String mime = contentType != null ? contentType.toString() : MediaType.IMAGE_JPEG_VALUE;
            return "data:" + mime + ";base64," + Base64.getEncoder().encodeToString(bytes);
        } catch (Exception e) {
            // 404 = no photo set; any other error is non-fatal.
            return null;
        }
    }

    private JWTClaimsSet validateIdToken(String idToken, String expectedNonce) {
        JWTClaimsSet claims;
        try {
            // Verifies RS256 signature against the tenant JWKS and the aud/exp claims.
            claims = jwtProcessor().process(idToken, null);
        } catch (MicrosoftAuthException e) {
            throw e;
        } catch (Exception e) {
            throw new MicrosoftAuthException("invalid_token", "ID token validation failed: " + e.getMessage());
        }

        // Nonce binds this token to the login request we initiated (replay protection).
        String nonce = stringClaim(claims, "nonce");
        if (expectedNonce == null || !expectedNonce.equals(nonce)) {
            throw new MicrosoftAuthException("invalid_token", "ID token nonce did not match the login request.");
        }

        // Issuer must be the tenant that actually issued the token: .../{tid}/v2.0
        String tid = stringClaim(claims, "tid");
        String issuer = claims.getIssuer();
        if (tid == null || issuer == null
                || !issuer.equals("https://login.microsoftonline.com/" + tid + "/v2.0")) {
            throw new MicrosoftAuthException("invalid_token", "ID token issuer is invalid.");
        }

        // If a specific tenant GUID is configured, the token must come from exactly that tenant.
        String configuredTenant = props.getTenantId();
        if (configuredTenant != null && configuredTenant.matches("[0-9a-fA-F-]{36}")
                && !configuredTenant.equalsIgnoreCase(tid)) {
            throw new MicrosoftAuthException("invalid_token", "ID token came from an unexpected tenant.");
        }

        return claims;
    }

    private ConfigurableJWTProcessor<SecurityContext> jwtProcessor() {
        ConfigurableJWTProcessor<SecurityContext> local = jwtProcessor;
        if (local == null) {
            synchronized (this) {
                local = jwtProcessor;
                if (local == null) {
                    try {
                        DefaultJWTProcessor<SecurityContext> processor = new DefaultJWTProcessor<>();
                        JWKSource<SecurityContext> jwkSource =
                                new RemoteJWKSet<>(new URL(authority() + "/discovery/v2.0/keys"));
                        JWSKeySelector<SecurityContext> keySelector =
                                new JWSVerificationKeySelector<>(JWSAlgorithm.RS256, jwkSource);
                        processor.setJWSKeySelector(keySelector);
                        // Require aud == clientId and the standard registered claims; exp is checked automatically.
                        processor.setJWTClaimsSetVerifier(new DefaultJWTClaimsVerifier<>(
                                new JWTClaimsSet.Builder().audience(props.getClientId()).build(),
                                new HashSet<>(Arrays.asList("aud", "exp", "iat", "iss", "sub"))));
                        jwtProcessor = processor;
                        local = processor;
                    } catch (Exception e) {
                        throw new MicrosoftAuthException("config_error",
                                "Could not initialise Microsoft token validation: " + e.getMessage());
                    }
                }
            }
        }
        return local;
    }

    // ---------------------------------------------------------------------
    // PKCE / state / nonce helpers
    // ---------------------------------------------------------------------

    /** URL-safe random token from {@code numBytes} of secure randomness (no padding). */
    public static String randomUrlToken(int numBytes) {
        byte[] bytes = new byte[numBytes];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** S256 PKCE code challenge for the given verifier. */
    public static String codeChallengeFor(String codeVerifier) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(codeVerifier.getBytes(StandardCharsets.US_ASCII));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (Exception e) {
            throw new MicrosoftAuthException("config_error", "Unable to compute PKCE challenge: " + e.getMessage());
        }
    }

    /** Constant-time comparison for OAuth state validation. */
    public static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }

    private static String stringClaim(JWTClaimsSet claims, String name) {
        try {
            return claims.getStringClaim(name);
        } catch (Exception e) {
            return null;
        }
    }

    private static String firstNonBlank(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) {
                return v;
            }
        }
        return null;
    }

    private static String asString(Object value) {
        return value == null ? null : value.toString();
    }

    /** Tokens returned from the authorization-code exchange. */
    private record TokenResponse(String idToken, String accessToken) {
    }

    /** Subset of the Microsoft Graph /me profile we use. */
    private record GraphProfile(String displayName, String email, String department,
                                String phone, String employeeId) {
    }
}
