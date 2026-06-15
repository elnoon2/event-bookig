const { Client, LocalAuth, MessageMedia } = require('whatsapp-web.js');
const qrcode = require('qrcode-terminal');
const express = require('express');
const fs = require('fs');
const path = require('path');

const app = express();
const port = process.env.PORT || 3000;
const SESSION_PATH = path.resolve(__dirname, 'sessions');

app.use(express.json());

let client = null;
let isReady = false;
let isInitializing = false;
let reconnectAttempts = 0;
let latestQr = null;
const MAX_RECONNECT_ATTEMPTS = 5;

// ─── Logging helper ───────────────────────────────────────────────────────────
function log(level, area, message, extra) {
    const ts = new Date().toISOString();
    const prefix = `[${ts}] [${level}] [${area}]`;
    if (extra !== undefined) {
        console.log(`${prefix} ${message}`, extra);
    } else {
        console.log(`${prefix} ${message}`);
    }
}

// ─── Phone number formatting (Egypt-centric) ─────────────────────────────────
function formatPhoneNumber(raw) {
    if (!raw) return null;

    // Strip everything except digits and leading '+'
    let phone = String(raw).replace(/[\s\-()]/g, '');
    
    log('DEBUG', 'PHONE', `Formatting phone: raw="${raw}", stripped="${phone}"`);

    // Remove leading '+' for uniform processing
    if (phone.startsWith('+')) {
        phone = phone.substring(1);
    }

    // Remove leading '00' international prefix (e.g., 0020...)
    if (phone.startsWith('00')) {
        phone = phone.substring(2);
    }

    // Egyptian local numbers: 01xxxxxxxxx (11 digits starting with 01)
    if (phone.startsWith('01') && phone.length === 11) {
        phone = '2' + phone;               // 01x -> 201x  (12 digits)
    }
    // Edge case: someone passes 1xxxxxxxxxx (10 digits starting with 1, no leading 0)
    else if (phone.startsWith('1') && phone.length === 10) {
        phone = '20' + phone;              // 1x -> 201x   (12 digits)
    }
    // Already has country code 20 and correct length
    else if (phone.startsWith('20') && phone.length === 12) {
        // already correct
    }
    // If 11 digits starting with 0 (non-Egyptian or wrong), strip leading 0 and prepend 20
    else if (phone.startsWith('0') && phone.length === 11) {
        phone = '20' + phone.substring(1);
    }

    // Final validation: must be all digits and at least 10 chars
    if (!/^\d{10,15}$/.test(phone)) {
        log('WARN', 'PHONE', `Phone number "${raw}" could not be normalized to a valid format. Result: "${phone}"`);
        return phone; // return as-is, let WhatsApp reject if invalid
    }

    log('INFO', 'PHONE', `Formatted phone: "${raw}" → "${phone}"`);
    return phone;
}

// ─── Session directory helpers ────────────────────────────────────────────────

// Clean up stale lock files that prevent session reuse — but NEVER delete
// the session data itself.
function cleanStaleLockFiles() {
    const sessionDir = path.join(SESSION_PATH, 'session');
    if (!fs.existsSync(sessionDir)) {
        log('INFO', 'SESSION', `Session directory does not exist yet: ${sessionDir}`);
        return;
    }
    log('INFO', 'SESSION', `Cleaning stale lock files in: ${sessionDir}`);

    const filesToClean = [
        'SingletonLock',
        'SingletonCookie',
        'SingletonSocket',
        path.join('Default', 'Lockfile'),
        path.join('Default', 'Preferences.bad')
    ];

    filesToClean.forEach(file => {
        const fullPath = path.join(sessionDir, file);
        if (fs.existsSync(fullPath)) {
            try {
                fs.unlinkSync(fullPath);
                log('INFO', 'SESSION', `Removed stale lock file: ${file}`);
            } catch (err) {
                log('WARN', 'SESSION', `Could not remove lock file ${file}: ${err.message}`);
            }
        }
    });
}

// Clear session directory ONLY on explicit logout or auth failure.
function clearSessionDirectory() {
    const sessionDir = path.join(SESSION_PATH, 'session');
    if (fs.existsSync(sessionDir)) {
        try {
            log('WARN', 'SESSION', `DELETING session directory (logout/auth failure): ${sessionDir}`);
            fs.rmSync(sessionDir, { recursive: true, force: true });
            log('INFO', 'SESSION', 'Session directory deleted successfully.');
        } catch (err) {
            log('ERROR', 'SESSION', `Failed to delete session directory: ${err.message}`);
        }
    }
}

// Check if a saved session exists
function hasExistingSession() {
    const sessionDir = path.join(SESSION_PATH, 'session');
    const defaultDir = path.join(sessionDir, 'Default');
    const exists = fs.existsSync(defaultDir);
    log('INFO', 'SESSION', `Session check — path="${sessionDir}", Default/ exists=${exists}`);
    return exists;
}

// ─── WhatsApp Client ──────────────────────────────────────────────────────────

async function startWhatsAppClient() {
    if (isInitializing) {
        log('WARN', 'INIT', 'Client initialization already in progress — skipping.');
        return;
    }
    isInitializing = true;
    isReady = false;

    log('INFO', 'INIT', '═══════════════════════════════════════════════════');
    log('INFO', 'INIT', 'Preparing to initialize WhatsApp client...');
    log('INFO', 'INIT', `Session storage path: ${SESSION_PATH}`);
    log('INFO', 'INIT', `Existing session found: ${hasExistingSession()}`);

    // 1. Clean stale locks — session DATA is preserved
    cleanStaleLockFiles();

    const puppeteerOpts = {
        headless: true,
        args: [
            '--no-sandbox',
            '--disable-setuid-sandbox',
            '--disable-dev-shm-usage',
            '--disable-accelerated-2d-canvas',
            '--no-first-run',
            '--no-zygote',
            '--disable-gpu',
            '--disable-extensions',
            '--disable-features=FirstPartySets',
            '--disable-features=LayoutNG'
        ]
    };
    if (process.env.PUPPETEER_EXECUTABLE_PATH) {
        puppeteerOpts.executablePath = process.env.PUPPETEER_EXECUTABLE_PATH;
    }

    // 2. Build client with LocalAuth for persistent sessions
    client = new Client({
        authStrategy: new LocalAuth({
            dataPath: SESSION_PATH
        }),
        puppeteer: puppeteerOpts
    });

    // ── Event handlers ──

    client.on('qr', async (qr) => {
        latestQr = qr;
        const pairingPhone = process.env.PAIRING_PHONE_NUMBER;
        if (pairingPhone) {
            log('INFO', 'AUTH', `PAIRING MODE ENABLED for phone: ${pairingPhone}`);
            try {
                const formattedPhone = formatPhoneNumber(pairingPhone);
                if (formattedPhone) {
                    const code = await client.requestPairingCode(formattedPhone);
                    log('INFO', 'AUTH', '════════════════════════════════════════════════');
                    log('INFO', 'AUTH', `🔑 YOUR WHATSAPP PAIRING CODE IS: ${code}`);
                    log('INFO', 'AUTH', 'Go to WhatsApp -> Linked Devices -> Link with phone number instead and enter this code.');
                    log('INFO', 'AUTH', '════════════════════════════════════════════════');
                } else {
                    log('ERROR', 'AUTH', `Failed to format pairing phone number: ${pairingPhone}`);
                }
            } catch (err) {
                log('ERROR', 'AUTH', `Failed to request pairing code: ${err.message}`);
                log('INFO', 'AUTH', '──── FALLBACK QR CODE ────');
                log('INFO', 'AUTH', 'Scan this QR code with WhatsApp → Settings → Linked Devices:');
                qrcode.generate(qr, { small: true });
            }
        } else {
            log('INFO', 'AUTH', '──── NEW QR CODE ────');
            log('INFO', 'AUTH', 'Scan this QR code with WhatsApp → Settings → Linked Devices:');
            qrcode.generate(qr, { small: true });
        }
        reconnectAttempts = 0;
    });

    client.on('loading_screen', (percent, message) => {
        log('INFO', 'AUTH', `Loading WhatsApp: ${percent}% — ${message || ''}`);
    });

    client.on('authenticated', () => {
        log('INFO', 'AUTH', '✅ WhatsApp Authenticated — session will be saved to disk.');
        log('INFO', 'AUTH', `Session path: ${SESSION_PATH}`);
    });

    client.on('ready', () => {
        log('INFO', 'READY', '════════════════════════════════════════════════');
        log('INFO', 'READY', '✅ WhatsApp Service is READY and connected!');
        log('INFO', 'READY', 'Messages can now be sent via POST /send-message');
        log('INFO', 'READY', '════════════════════════════════════════════════');
        isReady = true;
        isInitializing = false;
        reconnectAttempts = 0;
    });

    client.on('auth_failure', async (msg) => {
        log('ERROR', 'AUTH', `❌ Authentication failure: ${msg}`);
        isReady = false;
        isInitializing = false;

        try { await client.destroy(); } catch (e) {
            log('ERROR', 'AUTH', `Error destroying client after auth failure: ${e.message}`);
        }

        // Auth failure means the saved session is invalid — clear it
        clearSessionDirectory();

        log('INFO', 'AUTH', 'Will retry initialization in 10 seconds...');
        setTimeout(startWhatsAppClient, 10000);
    });

    client.on('disconnected', async (reason) => {
        log('WARN', 'DISCONNECT', `WhatsApp disconnected. Reason: ${reason}`);
        isReady = false;
        isInitializing = false;

        try {
            await client.destroy();
            log('INFO', 'DISCONNECT', 'Client destroyed after disconnect.');
        } catch (e) {
            log('ERROR', 'DISCONNECT', `Error destroying client: ${e.message}`);
        }

        if (reason === 'LOGOUT') {
            log('WARN', 'DISCONNECT', 'Explicit LOGOUT — clearing session data.');
            clearSessionDirectory();
            reconnectAttempts = 0;
        }

        // Attempt reconnection
        if (reconnectAttempts < MAX_RECONNECT_ATTEMPTS) {
            reconnectAttempts++;
            const delay = 5000 * reconnectAttempts;
            log('INFO', 'DISCONNECT', `Reconnection attempt ${reconnectAttempts}/${MAX_RECONNECT_ATTEMPTS} in ${delay / 1000}s...`);
            setTimeout(startWhatsAppClient, delay);
        } else {
            log('ERROR', 'DISCONNECT', 'Maximum reconnection attempts reached. Restart the service manually.');
        }
    });

    // 3. Initialize
    try {
        log('INFO', 'INIT', 'Calling client.initialize()...');
        await client.initialize();
    } catch (err) {
        log('ERROR', 'INIT', `Initialization error: ${err.message}`);
        log('ERROR', 'INIT', `Stack: ${err.stack}`);
        isInitializing = false;
        if (reconnectAttempts < MAX_RECONNECT_ATTEMPTS) {
            reconnectAttempts++;
            log('INFO', 'INIT', `Will retry in 5 seconds (attempt ${reconnectAttempts})...`);
            setTimeout(startWhatsAppClient, 5000);
        }
    }
}

// ─── API Endpoints ────────────────────────────────────────────────────────────

app.post('/send-message', async (req, res) => {
    const { phone, message, mediaUrl } = req.body;

    log('INFO', 'API', '────── New /send-message request ──────');
    log('INFO', 'API', `Phone: ${phone}`);
    log('INFO', 'API', `Message length: ${message ? message.length : 0}`);
    log('INFO', 'API', `Has media: ${!!mediaUrl}`);
    if (mediaUrl) log('INFO', 'API', `Media URL: ${mediaUrl}`);

    if (!isReady) {
        log('WARN', 'API', 'Service not ready — returning 503.');
        return res.status(503).json({
            success: false,
            message: 'WhatsApp service is not ready yet. Has the QR code been scanned?'
        });
    }

    if (!phone || (!message && !mediaUrl)) {
        log('WARN', 'API', 'Missing phone or message/media — returning 400.');
        return res.status(400).json({
            success: false,
            message: 'Phone and (message or mediaUrl) are required.'
        });
    }

    try {
        const cleanedPhone = formatPhoneNumber(phone);
        const chatId = cleanedPhone + '@c.us';
        log('INFO', 'API', `Formatted chatId: ${chatId}`);

        if (mediaUrl) {
            log('INFO', 'API', 'Fetching media from URL...');
            try {
                const media = await MessageMedia.fromUrl(mediaUrl, { unsafeMime: true });
                log('INFO', 'API', 'Media fetched successfully. Sending with caption...');
                await client.sendMessage(chatId, media, { caption: message });
                log('INFO', 'API', `✅ Media message sent to ${chatId}`);
            } catch (mediaError) {
                log('ERROR', 'API', `Media fetch/send failed: ${mediaError.message}`);
                log('INFO', 'API', 'Falling back to text-only message...');
                await client.sendMessage(chatId, message);
                log('INFO', 'API', `✅ Text fallback message sent to ${chatId}`);
            }
        } else {
            log('INFO', 'API', 'Sending text message...');
            await client.sendMessage(chatId, message);
            log('INFO', 'API', `✅ Text message sent to ${chatId}`);
        }

        res.json({ success: true, message: 'Message sent successfully.' });
    } catch (error) {
        log('ERROR', 'API', `❌ CRITICAL ERROR sending message: ${error.message}`);
        log('ERROR', 'API', `Stack: ${error.stack}`);
        res.status(500).json({
            success: false,
            message: 'Failed to send WhatsApp message.',
            error: error.message
        });
    }
});

app.get('/status', (req, res) => {
    const sessionExists = hasExistingSession();
    log('DEBUG', 'API', `GET /status — ready=${isReady}, sessionExists=${sessionExists}`);
    res.json({
        ready: isReady,
        sessionExists,
        sessionPath: SESSION_PATH
    });
});

app.get('/qr', (req, res) => {
    if (isReady) {
        return res.send(`
            <!DOCTYPE html>
            <html>
            <head>
                <title>WhatsApp Connected</title>
                <style>
                    body { font-family: Arial, sans-serif; display: flex; align-items: center; justify-content: center; height: 100vh; margin: 0; background-color: #f0f2f5; }
                    .container { background: white; padding: 40px; border-radius: 10px; box-shadow: 0 4px 6px rgba(0,0,0,0.1); text-align: center; }
                    h2 { color: #075e54; margin-bottom: 10px; }
                    p { color: #666; }
                </style>
            </head>
            <body>
                <div class="container">
                    <h2>✅ WhatsApp is Connected!</h2>
                    <p>The service is running and ready to send notifications.</p>
                </div>
            </body>
            </html>
        `);
    }
    if (!latestQr) {
        return res.send(`
            <!DOCTYPE html>
            <html>
            <head>
                <title>WhatsApp Connecting</title>
                <meta http-equiv="refresh" content="5">
                <style>
                    body { font-family: Arial, sans-serif; display: flex; align-items: center; justify-content: center; height: 100vh; margin: 0; background-color: #f0f2f5; }
                    .container { background: white; padding: 40px; border-radius: 10px; box-shadow: 0 4px 6px rgba(0,0,0,0.1); text-align: center; }
                    h2 { color: #e57373; margin-bottom: 10px; }
                    p { color: #666; }
                </style>
            </head>
            <body>
                <div class="container">
                    <h2>⏳ Please Wait...</h2>
                    <p>Generating pairing session. This page will refresh automatically.</p>
                </div>
            </body>
            </html>
        `);
    }
    
    res.send(`
        <!DOCTYPE html>
        <html>
        <head>
            <title>Scan WhatsApp QR Code</title>
            <script src="https://cdnjs.cloudflare.com/ajax/libs/qrcodejs/1.0.0/qrcode.min.js"></script>
            <style>
                body {
                    font-family: Arial, sans-serif;
                    display: flex;
                    flex-direction: column;
                    align-items: center;
                    justify-content: center;
                    height: 100vh;
                    margin: 0;
                    background-color: #f0f2f5;
                }
                .container {
                    background: white;
                    padding: 30px;
                    border-radius: 10px;
                    box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
                    text-align: center;
                }
                #qrcode {
                    display: flex;
                    justify-content: center;
                    margin: 20px 0;
                }
                h2 { color: #075e54; }
                p { color: #555; }
            </style>
        </head>
        <body>
            <div class="container">
                <h2>Scan this QR Code with WhatsApp</h2>
                <p>Go to WhatsApp -> Linked Devices -> Link a Device</p>
                <div id="qrcode"></div>
                <p style="font-size: 12px; color: #888;">Will refresh automatically when a new code is generated.</p>
            </div>
            <script>
                new QRCode(document.getElementById("qrcode"), {
                    text: "${latestQr}",
                    width: 256,
                    height: 256,
                    colorDark : "#000000",
                    colorLight : "#ffffff",
                    correctLevel : QRCode.CorrectLevel.H
                });
                
                // Refresh automatically every 15 seconds to fetch new QR if regenerated
                setTimeout(() => {
                    location.reload();
                }, 15000);
            </script>
        </body>
        </html>
    `);
});

// ─── Server start ─────────────────────────────────────────────────────────────

const server = app.listen(port, () => {
    log('INFO', 'SERVER', `═══════════════════════════════════════════════════`);
    log('INFO', 'SERVER', `WhatsApp Microservice listening at http://localhost:${port}`);
    log('INFO', 'SERVER', `Session storage: ${SESSION_PATH}`);
    log('INFO', 'SERVER', `═══════════════════════════════════════════════════`);
    startWhatsAppClient();
});

server.on('error', (err) => {
    if (err.code === 'EADDRINUSE') {
        log('ERROR', 'SERVER', `Port ${port} is already in use! Another instance may be running.`);
        process.exit(1);
    } else {
        log('ERROR', 'SERVER', `Express server error: ${err.message}`);
    }
});

// ─── Graceful shutdown ────────────────────────────────────────────────────────

async function gracefulShutdown(signal) {
    log('INFO', 'SHUTDOWN', `Received ${signal}. Starting graceful shutdown...`);
    isReady = false;
    isInitializing = false;

    if (client) {
        try {
            log('INFO', 'SHUTDOWN', 'Destroying WhatsApp client (session data is preserved on disk)...');
            await client.destroy();
            log('INFO', 'SHUTDOWN', 'WhatsApp client destroyed.');
        } catch (err) {
            log('ERROR', 'SHUTDOWN', `Error destroying client: ${err.message}`);
        }
    }

    server.close(() => {
        log('INFO', 'SHUTDOWN', 'Express server closed.');
        process.exit(0);
    });

    setTimeout(() => {
        log('ERROR', 'SHUTDOWN', 'Graceful shutdown timed out. Force exiting...');
        process.exit(1);
    }, 10000);
}

process.on('SIGINT', () => gracefulShutdown('SIGINT'));
process.on('SIGTERM', () => gracefulShutdown('SIGTERM'));

process.on('unhandledRejection', (reason, promise) => {
    log('ERROR', 'PROCESS', `Unhandled Rejection: ${reason}`);
});

process.on('uncaughtException', (err) => {
    log('ERROR', 'PROCESS', `Uncaught Exception: ${err.message}`);
    log('ERROR', 'PROCESS', `Stack: ${err.stack}`);
    if (err.message && (err.message.includes('EBUSY') || err.message.includes('locked'))) {
        gracefulShutdown('Uncaught EBUSY Exception');
    }
});
