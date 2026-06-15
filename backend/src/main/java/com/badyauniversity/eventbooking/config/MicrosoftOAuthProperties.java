package com.badyauniversity.eventbooking.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration for "Sign in with Microsoft" (OAuth2 / OpenID Connect).
 *
 * Values are bound from {@code app.microsoft.*} in application.properties, which in turn read the
 * environment variables MICROSOFT_CLIENT_ID / MICROSOFT_CLIENT_SECRET / MICROSOFT_TENANT_ID /
 * MICROSOFT_REDIRECT_URI. When the client id or secret is missing the feature reports itself as
 * disabled so the rest of the application keeps working unchanged.
 */
@Component
@ConfigurationProperties(prefix = "app.microsoft")
public class MicrosoftOAuthProperties {

    /** Application (client) ID from the Entra app registration. */
    private String clientId = "";

    /** Client secret value from the Entra app registration. Never sent to the browser. */
    private String clientSecret = "";

    /**
     * Tenant: a directory (tenant) GUID, or one of "common", "organizations", "consumers".
     * Default "common" makes the app multi-tenant and accepts personal Microsoft accounts as well as
     * work/school (Microsoft 365 / university) accounts.
     */
    private String tenantId = "common";

    /** Redirect URI registered in Entra; must exactly match the callback endpoint. */
    private String redirectUri = "http://localhost:5000/api/auth/microsoft/callback";

    /**
     * OAuth scopes. "openid profile email" yields the id token claims; "User.Read" grants a Microsoft
     * Graph access token used to read the user's profile and photo.
     */
    private String scopes = "openid profile email User.Read";

    /** Only accounts whose email ends with @{this} may sign in. Empty = allow any verified email. */
    private String allowedEmailDomain = "";

    /** Frontend page the user is returned to after the OAuth round-trip. */
    private String postLoginRedirect = "/index.html";

    /** True only when both the client id and secret are configured. */
    public boolean isEnabled() {
        return clientId != null && !clientId.isBlank()
                && clientSecret != null && !clientSecret.isBlank();
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    public void setRedirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
    }

    public String getScopes() {
        return scopes;
    }

    public void setScopes(String scopes) {
        this.scopes = scopes;
    }

    public String getAllowedEmailDomain() {
        return allowedEmailDomain;
    }

    public void setAllowedEmailDomain(String allowedEmailDomain) {
        this.allowedEmailDomain = allowedEmailDomain;
    }

    public String getPostLoginRedirect() {
        return postLoginRedirect;
    }

    public void setPostLoginRedirect(String postLoginRedirect) {
        this.postLoginRedirect = postLoginRedirect;
    }
}
