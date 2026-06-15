/**
 * Admin panel settings (edit as needed).
 * Admin credentials are configured on the backend via the APP_ADMIN_EMAIL / APP_ADMIN_PASSWORD
 * environment variables (see application.properties). Do not store credentials in this file.
 */
window.ADMIN_CONFIG = {
    API_BASE_URL: 'http://localhost:5000',
    /**
     * Base URL where the frontend pages are hosted (used to build QR links).
     * Example: 'http://localhost:5500' or 'https://your-domain.com'
     *
     * If left empty, the admin page will auto-detect when running on http(s).
     */
    SITE_BASE_URL: ''
};
